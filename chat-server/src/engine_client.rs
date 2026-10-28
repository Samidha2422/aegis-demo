use crate::models::{EngineEventEnvelope, EngineVerdict};
use reqwest::Client;
use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::time::Duration;
use tracing::{error, info, warn};

#[derive(Clone)]
pub struct EngineClient {
    client: Client,
    base_url: String,
    api_key: String,
    circuit_open: Arc<AtomicBool>,
}

impl EngineClient {
    pub fn new(base_url: String) -> Self {
        let api_key =
            std::env::var("AEGIS_ENGINE_API_KEY").unwrap_or_else(|_| "demo_key".to_string());
        let client = Client::builder()
            .timeout(Duration::from_millis(1500))
            .build()
            .unwrap_or_default();

        Self {
            client,
            base_url,
            api_key,
            circuit_open: Arc::new(AtomicBool::new(false)),
        }
    }

    /// Dispatches an event to the Harassment Risk Engine with 1 retry and circuit-breaker handling
    pub async fn score_event(&self, event: &EngineEventEnvelope) -> Option<EngineVerdict> {
        if self.circuit_open.load(Ordering::Relaxed) {
            warn!("Engine circuit breaker is OPEN, skipping engine call");
            return None;
        }

        let url = format!("{}/v1/events", self.base_url);

        for attempt in 0..2 {
            let res = self
                .client
                .post(&url)
                .header("Authorization", format!("Bearer {}", self.api_key))
                .header("Content-Type", "application/json")
                .json(event)
                .send()
                .await;

            match res {
                Ok(response) => {
                    if response.status().is_success() {
                        if let Ok(verdict) = response.json::<EngineVerdict>().await {
                            return Some(verdict);
                        }
                    } else {
                        warn!("Engine responded with HTTP status: {}", response.status());
                    }
                }
                Err(err) => {
                    warn!("Engine request attempt {} failed: {}", attempt + 1, err);
                }
            }

            tokio::time::sleep(Duration::from_millis(100)).await;
        }

        error!("Engine unreachable after retries; marking circuit degraded");
        None
    }

    /// Evaluates a pre-send draft message
    pub async fn score_draft(&self, text: &str) -> Option<EngineVerdict> {
        let url = format!("{}/v1/events/nudge", self.base_url);
        let mut body = HashMap::new();
        body.insert("draft", text);

        let res = self
            .client
            .post(&url)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .json(&body)
            .send()
            .await;

        match res {
            Ok(response) if response.status().is_success() => {
                response.json::<EngineVerdict>().await.ok()
            }
            _ => None,
        }
    }

    /// Reports a refusal (block, mute, stop requested)
    pub async fn report_refusal(&self, user_id: &str, target_id: &str, action: &str) {
        let url = format!("{}/v1/events/refusal", self.base_url);
        let mut body = HashMap::new();
        body.insert("user_id", user_id);
        body.insert("target_id", target_id);
        body.insert("action", action);

        let _ = self
            .client
            .post(&url)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .json(&body)
            .send()
            .await;
        info!("Reported refusal action '{}' for user {}", action, user_id);
    }
}
