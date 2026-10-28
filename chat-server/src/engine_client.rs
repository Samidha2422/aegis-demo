use crate::models::{EngineEventEnvelope, EngineNudge, EngineSignal, EngineVerdict};
use axum::{
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use reqwest::{Client, RequestBuilder};
use serde::{de::DeserializeOwned, Deserialize, Serialize};
use std::{collections::HashMap, time::Duration};

#[derive(Clone)]
pub struct EngineClient {
    client: Client,
    base_url: String,
    api_key: String,
}

// Never expose upstream bodies, URLs, credentials or request payloads in errors/logs.
#[derive(Debug, thiserror::Error)]
pub enum EngineError {
    #[error("Engine connection failed or timed out")]
    Unavailable,
    #[error("Engine rejected the request (HTTP {0})")]
    Rejected(u16),
    #[error("Engine returned an incompatible response")]
    Contract,
    #[error("Set AEGIS_ENGINE_API_KEY on the chat server to a key in HRE_API_KEYS")]
    Configuration,
}

impl IntoResponse for EngineError {
    fn into_response(self) -> Response {
        let status = match self {
            Self::Rejected(400 | 413 | 422) => StatusCode::UNPROCESSABLE_ENTITY,
            Self::Rejected(429) => StatusCode::TOO_MANY_REQUESTS,
            Self::Unavailable | Self::Configuration => StatusCode::SERVICE_UNAVAILABLE,
            _ => StatusCode::BAD_GATEWAY,
        };
        (
            status,
            Json(serde_json::json!({"error": "engine_error", "detail": self.to_string()})),
        )
            .into_response()
    }
}

#[derive(Deserialize)]
struct AnalyzeResponse {
    severity: f32,
    tier: String,
    reasons: Vec<Reason>,
    signals: HashMap<String, EngineSignal>,
    recommended_action: String,
    simulated: bool,
    degraded: Vec<String>,
    model_versions: HashMap<String, String>,
    #[serde(default)]
    nudge: Option<EngineNudge>,
}

#[derive(Deserialize)]
struct Reason {
    text: String,
}

impl From<AnalyzeResponse> for EngineVerdict {
    fn from(response: AnalyzeResponse) -> Self {
        Self {
            severity: response.severity,
            tier: response.tier,
            reasons: response.reasons.into_iter().map(|r| r.text).collect(),
            signals: response
                .signals
                .iter()
                .map(|(k, v)| (k.clone(), v.score))
                .collect(),
            recommended_action: response.recommended_action,
            simulated: response.simulated,
            degraded: response.degraded,
            model_versions: response.model_versions,
            modules: response.signals,
            nudge: response.nudge,
        }
    }
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ModelHealth {
    pub name: String,
    pub version: Option<String>,
    pub hash_verified: bool,
    pub backend: String,
    pub degraded: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct EngineHealth {
    pub status: String,
    pub version: String,
    pub mock: bool,
    pub degraded: Vec<String>,
}

#[derive(Serialize)]
pub struct EngineStatus {
    pub health: EngineHealth,
    pub models: Vec<ModelHealth>,
}

impl EngineClient {
    pub fn new(base_url: String) -> Self {
        Self::with_key(
            base_url,
            std::env::var("AEGIS_ENGINE_API_KEY").unwrap_or_default(),
        )
    }

    pub(crate) fn with_key(base_url: String, api_key: String) -> Self {
        Self {
            client: Client::new(),
            base_url: base_url.trim_end_matches('/').to_owned(),
            api_key,
        }
    }

    fn request(&self, method: reqwest::Method, path: &str) -> Result<RequestBuilder, EngineError> {
        if self.api_key.trim().is_empty() {
            return Err(EngineError::Configuration);
        }
        Ok(self
            .client
            .request(method, format!("{}{path}", self.base_url))
            .timeout(Duration::from_secs(30))
            .header("X-API-Key", &self.api_key))
    }

    async fn response(&self, request: RequestBuilder) -> Result<reqwest::Response, EngineError> {
        let response = request.send().await.map_err(|_| EngineError::Unavailable)?;
        if !response.status().is_success() {
            return Err(EngineError::Rejected(response.status().as_u16()));
        }
        Ok(response)
    }

    async fn json<T: DeserializeOwned>(&self, request: RequestBuilder) -> Result<T, EngineError> {
        self.response(request)
            .await?
            .json()
            .await
            .map_err(|_| EngineError::Contract)
    }

    pub async fn score_event(
        &self,
        event: &EngineEventEnvelope,
    ) -> Result<EngineVerdict, EngineError> {
        let response: AnalyzeResponse = self
            .json(
                self.request(reqwest::Method::POST, "/v1/analyze")?
                    .json(event),
            )
            .await?;
        Ok(response.into())
    }

    pub async fn score_image(
        &self,
        event: &EngineEventEnvelope,
        bytes: Vec<u8>,
        mime: &str,
    ) -> Result<EngineVerdict, EngineError> {
        let metadata = serde_json::to_string(event).map_err(|_| EngineError::Contract)?;
        let file = reqwest::multipart::Part::bytes(bytes)
            .file_name("upload")
            .mime_str(mime)
            .map_err(|_| EngineError::Contract)?;
        let form = reqwest::multipart::Form::new()
            .text("request", metadata)
            .part("image", file);
        let response: AnalyzeResponse = self
            .json(
                self.request(reqwest::Method::POST, "/v1/analyze/image")?
                    .multipart(form),
            )
            .await?;
        Ok(response.into())
    }

    pub async fn report_refusal(
        &self,
        user_id: &str,
        target_id: &str,
        action: &str,
    ) -> Result<(), EngineError> {
        // The refused sender is the target of the platform block action.
        self.response(
            self.request(reqwest::Method::POST, "/v1/events/refusal")?
                .json(&serde_json::json!({
                    "event_id": uuid::Uuid::new_v4(), "sender_id": target_id,
                    "recipient_id": user_id, "kind": action, "occurred_at": chrono::Utc::now(),
                    "platform": "demo-chat"
                })),
        )
        .await?;
        Ok(())
    }

    pub async fn report(&self, event_id: &str, reason: &str) -> Result<(), EngineError> {
        self.response(
            self.request(reqwest::Method::POST, "/v1/reports")?
                .json(&serde_json::json!({"event_id": event_id, "reason": reason})),
        )
        .await?;
        Ok(())
    }

    pub async fn status(&self) -> Result<EngineStatus, EngineError> {
        let health = self
            .json(self.request(reqwest::Method::GET, "/v1/health")?)
            .await?;
        let models = self
            .json(self.request(reqwest::Method::GET, "/v1/health/models")?)
            .await?;
        Ok(EngineStatus { health, models })
    }

    pub async fn export_evidence(&self, html: bool) -> Result<String, EngineError> {
        let request = if html {
            self.request(reqwest::Method::GET, "/v1/evidence/meera/html")?
        } else {
            self.request(reqwest::Method::POST, "/v1/evidence/export")?
                .json(&serde_json::json!({"subject_id": "meera"}))
        };
        self.response(request)
            .await?
            .text()
            .await
            .map_err(|_| EngineError::Contract)
    }

    pub async fn import_calls(&self, csv: &str) -> Result<serde_json::Value, EngineError> {
        self.json(self.request(reqwest::Method::POST, "/v1/calllogs/import")?.json(&serde_json::json!({
            "platform": "demo-chat", "conversation_id": "uploaded_calls", "csv": csv, "draft": false
        }))).await
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::{routing::post, Router};

    #[tokio::test]
    async fn analyze_uses_real_auth_envelope_and_preserves_status() {
        async fn analyze(
            headers: axum::http::HeaderMap,
            Json(event): Json<serde_json::Value>,
        ) -> Json<serde_json::Value> {
            assert_eq!(headers["x-api-key"], "test-key");
            assert!(headers.get("authorization").is_none());
            assert!(uuid::Uuid::parse_str(event["event_id"].as_str().unwrap()).is_ok());
            assert_eq!(event["channel"]["type"], "dm");
            assert_eq!(event["message"]["text"], "hello");
            assert_eq!(event["draft"], true);
            assert!(event.get("content").is_none());
            Json(serde_json::json!({
                "severity": 42, "tier": "review", "reasons": [{"code": "PERSISTENCE", "text": "Repeated contact", "weight": 0.5, "evidence": {}}],
                "signals": {"text": {"score": 0.4, "label": "toxicity", "status": "built", "backend": "heuristic", "confidence": 0.5, "details": {}}},
                "recommended_action": "warn_sender", "simulated": true,
                "degraded": ["text.onnx_missing"], "model_versions": {},
                "nudge": {"text": "Please revise", "rewrite_hint": "Be kind"}
            }))
        }
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();
        let server = tokio::spawn(async move {
            axum::serve(listener, Router::new().route("/v1/analyze", post(analyze)))
                .await
                .unwrap();
        });
        let client = EngineClient::with_key(format!("http://{addr}/"), "test-key".into());
        let mut event = EngineEventEnvelope::message("meera", "rohan", "chat_rohan", "hello");
        event.draft = true;
        let result = client.score_event(&event).await.unwrap();
        assert_eq!(result.reasons, ["Repeated contact"]);
        assert!(result.simulated);
        assert_eq!(result.modules["text"].backend, "heuristic");
        assert_eq!(result.degraded, ["text.onnx_missing"]);
        server.abort();
    }

    #[tokio::test]
    async fn missing_key_is_configuration_error_not_clean_verdict() {
        let client = EngineClient::with_key("http://127.0.0.1:1".into(), String::new());
        let event = EngineEventEnvelope::message("a", "b", "chat", "hello");
        assert!(matches!(
            client.score_event(&event).await,
            Err(EngineError::Configuration)
        ));
    }
}
