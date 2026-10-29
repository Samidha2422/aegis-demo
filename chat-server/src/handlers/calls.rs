use crate::{
    action_policy::ActionPolicy,
    models::{CallRecord, EngineCall, EngineEventEnvelope, WsEvent},
    state::AppState,
};
use axum::{extract::State, http::StatusCode, Json};
use serde::Deserialize;

fn de_opt_u32<'de, D>(deserializer: D) -> Result<Option<u32>, D::Error>
where
    D: serde::Deserializer<'de>,
{
    #[derive(Deserialize)]
    #[serde(untagged)]
    enum NumOrStr {
        Num(u32),
        Str(String),
    }
    match Option::<NumOrStr>::deserialize(deserializer)? {
        Some(NumOrStr::Num(n)) => Ok(Some(n)),
        Some(NumOrStr::Str(s)) => s.parse::<u32>().map(Some).map_err(serde::de::Error::custom),
        None => Ok(None),
    }
}

#[derive(Deserialize)]
pub struct SimulateCallRequest {
    pub caller_id: String,
    #[serde(default, deserialize_with = "de_opt_u32")]
    pub duration_secs: Option<u32>,
    pub status: Option<String>,
    pub transcript: Option<String>,
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
    let transcript_text = payload
        .transcript
        .as_deref()
        .unwrap_or("")
        .trim()
        .to_string();
    let has_transcript = !transcript_text.is_empty();
    let mut event = EngineEventEnvelope::message(&payload.caller_id, "meera", "demo_calls", "");
    event.kind = "call_event".into();
    event.channel.kind = "call".into();
    event.message = None;
    event.call = Some(EngineCall {
        direction: "incoming".into(),
        duration_s: duration_secs,
        answered: status == "answered",
        voicemail: has_transcript,
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

    // If a transcript is provided, evaluate it with the live neural text & threat models and fuse verdicts
    if has_transcript {
        let mut text_event = EngineEventEnvelope::message(
            &payload.caller_id,
            "meera",
            "chat_calls",
            &transcript_text,
        );
        text_event.draft = true;
        text_event.channel.kind = "dm".into();
        text_event.context.recipient_blocked_sender = event.context.recipient_blocked_sender;
        text_event.context.recipient_muted_sender = event.context.recipient_muted_sender;

        if let Ok(text_verdict) = state.engine_client.score_event(&text_event).await {
            if text_verdict.severity > safety.severity {
                safety.severity = text_verdict.severity;
                safety.tier = text_verdict.tier;
                safety.recommended_action = text_verdict.recommended_action;
            }
            for reason in text_verdict.reasons {
                if !safety.reasons.contains(&reason) {
                    safety.reasons.push(reason);
                }
            }
            for (k, v) in text_verdict.signals {
                safety.signals.insert(k, v);
            }
        }
    }
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
        transcript: if has_transcript {
            Some(transcript_text)
        } else {
            None
        },
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
    State(state): State<AppState>,
    multipart: axum::extract::Multipart,
) -> Result<Json<crate::models::SafeMessage>, StatusCode> {
    super::chats::upload(&state, None, multipart).await
}
