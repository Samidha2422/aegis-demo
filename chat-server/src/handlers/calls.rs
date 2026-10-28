use crate::{
    action_policy::ActionPolicy,
    models::{CallRecord, EngineCall, EngineEventEnvelope, WsEvent},
    state::AppState,
};
use axum::{extract::State, http::StatusCode, Json};
use serde::Deserialize;

#[derive(Deserialize)]
pub struct SimulateCallRequest {
    pub caller_id: String,
    pub duration_secs: Option<u32>,
    pub status: Option<String>,
}

pub async fn list_calls(State(state): State<AppState>) -> Json<Vec<CallRecord>> {
    Json(state.calls.read().await.clone())
}

pub async fn simulate_call(
    State(state): State<AppState>,
    Json(payload): Json<SimulateCallRequest>,
) -> Result<(StatusCode, Json<CallRecord>), StatusCode> {
    let duration_secs = payload.duration_secs.unwrap_or(0);
    let status = payload.status.unwrap_or_else(|| "missed".into());
    if !matches!(status.as_str(), "answered" | "missed" | "rejected")
        || payload.caller_id.trim().is_empty()
        || payload.caller_id.len() > 256
    {
        return Err(StatusCode::UNPROCESSABLE_ENTITY);
    }
    let mut event = EngineEventEnvelope::message(&payload.caller_id, "meera", "demo_calls", "");
    event.kind = "call_event".into();
    event.channel.kind = "call".into();
    event.message = None;
    event.call = Some(EngineCall {
        direction: "incoming".into(),
        duration_s: duration_secs,
        answered: status == "answered",
        voicemail: false,
    });
    event.context.recipient_blocked_sender = state
        .blocked_users
        .read()
        .await
        .contains(&("meera".into(), payload.caller_id.clone()));
    event.context.recipient_muted_sender = state
        .muted_users
        .read()
        .await
        .contains(&("meera".into(), payload.caller_id.clone()));
    let mut safety = match state.engine_client.score_event(&event).await {
        Ok(v) => ActionPolicy::apply(&v, "call", event.context.recipient_blocked_sender, 0),
        Err(e) => ActionPolicy::unscored(&e.to_string()),
    };
    // A synthetic call input remains labelled even when its assessment uses the live engine.
    safety.simulated = true;
    safety.tags.push("SIMULATED CALL".into());
    let record = CallRecord {
        id: event.event_id,
        caller_id: payload.caller_id,
        callee_id: "meera".into(),
        timestamp: event.occurred_at,
        duration_secs,
        status,
        safety,
    };
    state.calls.write().await.push(record.clone());
    let _ = state.tx.send(WsEvent {
        event: "call_event".into(),
        payload: serde_json::to_value(&record).unwrap_or_default(),
    });
    Ok((StatusCode::CREATED, Json(record)))
}

pub async fn import_call_log(
    State(state): State<AppState>,
    body: String,
) -> Result<Json<serde_json::Value>, crate::engine_client::EngineError> {
    if body.len() > 2 * 1024 * 1024 {
        return Err(crate::engine_client::EngineError::Rejected(413));
    }
    // Engine validates the whole CSV and HMACs identities; forward per-row outcomes, including failures.
    state.engine_client.import_calls(&body).await.map(Json)
}

pub async fn upload_recording(
    State(_state): State<AppState>,
) -> (StatusCode, Json<serde_json::Value>) {
    (
        StatusCode::NOT_IMPLEMENTED,
        Json(
            serde_json::json!({"status": "roadmap", "detail": "Recording upload is not connected; no audio was analyzed"}),
        ),
    )
}
