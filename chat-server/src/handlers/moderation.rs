use crate::{
    engine_client::{EngineError, EngineStatus},
    models::EngineEventEnvelope,
    state::AppState,
};
use axum::{
    body::Bytes,
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use serde::{Deserialize, Serialize};

#[derive(Deserialize)]
pub struct ReportRequest {
    pub message_id: Option<String>,
    pub reason: String,
}

#[derive(Deserialize)]
pub struct NudgeRequest {
    pub draft: String,
    pub chat_id: String,
    #[serde(default = "super::chats::default_sender")]
    pub sender_id: String,
}

#[derive(Serialize)]
pub struct NudgeResponse {
    pub severity: f32,
    pub tier: String,
    pub reasons: Vec<String>,
    pub recommended_action: String,
    pub nudge_text: Option<String>,
    pub simulated: bool,
    pub degraded: Vec<String>,
}

#[derive(Default, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct RefusalActor {
    pub actor_id: Option<String>,
}

async fn apply_refusal(
    state: &AppState,
    target: String,
    body: &[u8],
    action: &str,
) -> Result<StatusCode, EngineError> {
    let request = if body.is_empty() {
        RefusalActor::default()
    } else {
        serde_json::from_slice::<RefusalActor>(body).map_err(|_| EngineError::Rejected(422))?
    };
    let actor = request
        .actor_id
        .unwrap_or_else(super::chats::default_sender);
    if actor == target
        || target.trim().is_empty()
        || target.len() > 256
        || target.chars().any(char::is_control)
        || !state.personas.read().await.iter().any(|p| p.id == actor)
    {
        return Err(EngineError::Rejected(422));
    }
    state
        .engine_client
        .report_refusal(&actor, &target, action)
        .await?;
    let pair = (actor, target);
    match action {
        "block" => {
            state.blocked_users.write().await.insert(pair);
        }
        "unblock" => {
            state.blocked_users.write().await.remove(&pair);
        }
        "mute" => {
            state.muted_users.write().await.insert(pair);
        }
        _ => return Err(EngineError::Rejected(422)),
    }
    Ok(StatusCode::OK)
}

pub async fn engine_status(
    State(state): State<AppState>,
) -> Result<Json<EngineStatus>, EngineError> {
    state.engine_client.status().await.map(Json)
}

pub async fn block_user(
    State(state): State<AppState>,
    Path(id): Path<String>,
    body: Bytes,
) -> Result<StatusCode, EngineError> {
    apply_refusal(&state, id, &body, "block").await
}

pub async fn unblock_user(
    State(state): State<AppState>,
    Path(id): Path<String>,
    body: Bytes,
) -> Result<StatusCode, EngineError> {
    apply_refusal(&state, id, &body, "unblock").await
}

pub async fn mute_user(
    State(state): State<AppState>,
    Path(id): Path<String>,
    body: Bytes,
) -> Result<StatusCode, EngineError> {
    apply_refusal(&state, id, &body, "mute").await
}

pub async fn submit_report(
    State(state): State<AppState>,
    Json(payload): Json<ReportRequest>,
) -> Result<StatusCode, EngineError> {
    let event_id = payload.message_id.ok_or(EngineError::Rejected(422))?;
    state
        .engine_client
        .report(&event_id, &payload.reason)
        .await?;
    Ok(StatusCode::CREATED)
}

pub async fn check_nudge(
    State(state): State<AppState>,
    Json(payload): Json<NudgeRequest>,
) -> Result<Json<NudgeResponse>, EngineError> {
    if payload.draft.trim().is_empty() || payload.draft.len() > 32768 {
        return Err(EngineError::Rejected(422));
    }
    let mut event: EngineEventEnvelope = state
        .message_event(&payload.sender_id, &payload.chat_id, &payload.draft)
        .await
        .map_err(|_| EngineError::Rejected(422))?;
    event.draft = true;
    // No chat writes, alerts or broadcasts occur on this path.
    let v = state.engine_client.score_event(&event).await?;
    Ok(Json(NudgeResponse {
        severity: v.severity,
        tier: v.tier,
        reasons: v.reasons,
        recommended_action: v.recommended_action,
        nudge_text: v.nudge.map(|n| n.text),
        simulated: v.simulated,
        degraded: v.degraded,
    }))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn selected_actor_refusals_reach_engine_and_remain_directional() {
        use axum::{
            routing::{get, post},
            Router,
        };
        use std::sync::Arc;
        use tokio::sync::Mutex;

        async fn record(
            State(events): State<Arc<Mutex<Vec<serde_json::Value>>>>,
            Json(event): Json<serde_json::Value>,
        ) -> StatusCode {
            events.lock().await.push(event);
            StatusCode::NO_CONTENT
        }

        let events = Arc::new(Mutex::new(Vec::new()));
        let engine_listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let engine_url = format!("http://{}", engine_listener.local_addr().unwrap());
        let engine_app = Router::new()
            .route("/v1/events/refusal", post(record))
            .with_state(events.clone());
        let engine_task = tokio::spawn(async move {
            axum::serve(engine_listener, engine_app).await.unwrap();
        });
        let mut state = AppState::new(engine_url.clone());
        state.engine_client = Arc::new(crate::engine_client::EngineClient::with_key(
            engine_url,
            "test-key".into(),
        ));
        let chat_listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let chat_url = format!("http://{}", chat_listener.local_addr().unwrap());
        let app = Router::new()
            .route("/users/:id/block", post(block_user))
            .route("/users/:id/unblock", post(unblock_user))
            .route("/users/:id/mute", post(mute_user))
            .route("/chats", get(super::super::chats::list_chats))
            .with_state(state.clone());
        let chat_task = tokio::spawn(async move {
            axum::serve(chat_listener, app).await.unwrap();
        });
        let client = reqwest::Client::new();

        let response = client
            .post(format!("{chat_url}/users/meera/block"))
            .json(&serde_json::json!({"actor_id": "rohan"}))
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::OK);
        assert!(
            state
                .message_event("meera", "chat_rohan", "hello")
                .await
                .unwrap()
                .context
                .recipient_blocked_sender
        );
        assert!(
            !state
                .message_event("rohan", "chat_rohan", "hello")
                .await
                .unwrap()
                .context
                .recipient_blocked_sender
        );
        let threads: Vec<crate::models::ChatThread> = client
            .get(format!("{chat_url}/chats?actor_id=rohan"))
            .send()
            .await
            .unwrap()
            .json()
            .await
            .unwrap();
        assert_eq!(threads.len(), 1);
        assert_eq!(threads[0].participant.id, "meera");
        assert!(threads[0].participant.is_blocked);

        for action in ["mute", "unblock"] {
            let response = client
                .post(format!("{chat_url}/users/meera/{action}"))
                .json(&serde_json::json!({"actor_id": "rohan"}))
                .send()
                .await
                .unwrap();
            assert_eq!(response.status(), StatusCode::OK);
        }
        let context = state
            .message_event("meera", "chat_rohan", "hello")
            .await
            .unwrap()
            .context;
        assert!(!context.recipient_blocked_sender);
        assert!(context.recipient_muted_sender);
        assert!(
            !state
                .message_event("rohan", "chat_rohan", "hello")
                .await
                .unwrap()
                .context
                .recipient_muted_sender
        );

        // An old client sending no body still acts as Meera.
        assert_eq!(
            client
                .post(format!("{chat_url}/users/rohan/block"))
                .send()
                .await
                .unwrap()
                .status(),
            StatusCode::OK
        );
        for body in [r#"{"actor_id":"missing"}"#, r#"{"actor_id":"meera"}"#, "{"] {
            assert_eq!(
                client
                    .post(format!("{chat_url}/users/meera/block"))
                    .body(body)
                    .send()
                    .await
                    .unwrap()
                    .status(),
                StatusCode::UNPROCESSABLE_ENTITY
            );
        }
        let recorded = events.lock().await;
        assert_eq!(recorded.len(), 4);
        for (event, kind) in recorded.iter().take(3).zip(["block", "mute", "unblock"]) {
            assert_eq!(event["sender_id"], "meera");
            assert_eq!(event["recipient_id"], "rohan");
            assert_eq!(event["platform"], "demo-chat");
            assert_eq!(event["kind"], kind);
        }
        assert_eq!(recorded[3]["sender_id"], "rohan");
        assert_eq!(recorded[3]["recipient_id"], "meera");
        chat_task.abort();
        engine_task.abort();
    }

    #[tokio::test]
    async fn outgoing_draft_context_does_not_reverse_a_block() {
        let state = AppState::new("http://127.0.0.1:1".into());
        state
            .blocked_users
            .write()
            .await
            .insert(("meera".into(), "rohan".into()));
        let outgoing = state
            .message_event("meera", "chat_rohan", "hello")
            .await
            .unwrap();
        let incoming = state
            .message_event("rohan", "chat_rohan", "hello")
            .await
            .unwrap();
        assert!(!outgoing.context.recipient_blocked_sender);
        assert!(incoming.context.recipient_blocked_sender);
        assert!(state.chats.read().await.is_empty());
        assert!(state.alerts.read().await.is_empty());
    }
}
