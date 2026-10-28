use crate::models::{CallRecord, SafetyObject};
use crate::state::AppState;
use axum::{extract::State, http::StatusCode, Json};
use chrono::Utc;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Deserialize)]
pub struct SimulateCallRequest {
    pub caller_id: String,
    pub duration_secs: Option<u32>,
    pub status: Option<String>,
}

pub async fn list_calls(State(state): State<AppState>) -> Json<Vec<CallRecord>> {
    let mut calls = state.calls.read().await.clone();
    if calls.is_empty() {
        // Seed initial synthetic call records
        let now = Utc::now();
        // Rohan calls (unanswered, late-night)
        for i in 1..=5 {
            calls.push(CallRecord {
                id: format!("call_init_{}", i),
                caller_id: "unknown_number".to_string(),
                callee_id: "meera".to_string(),
                timestamp: now - chrono::Duration::minutes(15 * i),
                duration_secs: 0,
                status: "missed".to_string(),
                safety: SafetyObject {
                    severity: 75.0,
                    tier: "high".to_string(),
                    reasons: vec![
                        "Repeated unanswered call".to_string(),
                        "Overnight timing burst".to_string(),
                    ],
                    signals: std::collections::HashMap::new(),
                    recommended_action: "throttle".to_string(),
                    state: "Escalated".to_string(),
                    simulated: true,
                    tags: vec!["SIMULATED CALL LOG".to_string()],
                },
            });
        }
    }
    Json(calls)
}

pub async fn simulate_call(
    State(state): State<AppState>,
    Json(payload): Json<SimulateCallRequest>,
) -> (StatusCode, Json<CallRecord>) {
    let duration_secs = payload.duration_secs.unwrap_or(0);
    let status = payload.status.unwrap_or_else(|| "missed".to_string());
    let is_stalker = payload.caller_id.contains("unknown") || payload.caller_id == "rohan";

    let safety = if is_stalker {
        SafetyObject {
            severity: 80.0,
            tier: "high".to_string(),
            reasons: vec![
                "Multiple unanswered calls in short window".to_string(),
                "Pattern matches harassing frequency".to_string(),
            ],
            signals: std::collections::HashMap::new(),
            recommended_action: "throttle".to_string(),
            state: "Escalated".to_string(),
            simulated: true,
            tags: vec!["SIMULATED CALL".to_string()],
        }
    } else {
        SafetyObject::default()
    };

    let record = CallRecord {
        id: format!("call_{}", Uuid::new_v4().to_string().replace('-', "")),
        caller_id: payload.caller_id,
        callee_id: "meera".to_string(),
        timestamp: Utc::now(),
        duration_secs,
        status,
        safety,
    };

    {
        let mut calls = state.calls.write().await;
        calls.push(record.clone());
    }

    (StatusCode::CREATED, Json(record))
}

pub async fn import_call_log(
    State(state): State<AppState>,
    body: String,
) -> (StatusCode, Json<serde_json::Value>) {
    // Basic CSV parser
    let mut imported = 0;
    let mut calls = state.calls.write().await;
    for line in body.lines().skip(1) {
        let parts: Vec<&str> = line.split(',').map(|s| s.trim()).collect();
        if parts.len() >= 4 {
            let caller = parts[1].to_string();
            let duration = parts[3].parse::<u32>().unwrap_or(0);
            let status = if duration > 0 { "answered" } else { "missed" };
            let is_stalker = caller.contains("unknown");

            calls.push(CallRecord {
                id: format!("call_csv_{}", Uuid::new_v4().to_string().replace('-', "")),
                caller_id: caller,
                callee_id: "meera".to_string(),
                timestamp: Utc::now(),
                duration_secs: duration,
                status: status.to_string(),
                safety: if is_stalker {
                    SafetyObject {
                        severity: 85.0,
                        tier: "high".to_string(),
                        reasons: vec!["CSV Import: 40 calls overnight, 92% unanswered".to_string()],
                        signals: std::collections::HashMap::new(),
                        recommended_action: "throttle".to_string(),
                        state: "Escalated".to_string(),
                        simulated: true,
                        tags: vec!["SIMULATED CALL LOG".to_string()],
                    }
                } else {
                    SafetyObject::default()
                },
            });
            imported += 1;
        }
    }

    (
        StatusCode::OK,
        Json(serde_json::json!({ "imported_count": imported })),
    )
}

pub async fn upload_recording(
    State(_state): State<AppState>,
) -> (StatusCode, Json<serde_json::Value>) {
    // Transient voicemail upload
    (
        StatusCode::CREATED,
        Json(serde_json::json!({
            "status": "processed",
            "transcript": "Why aren't you answering my calls? You know you can't avoid this.",
            "tier": "high",
            "severity": 88.0,
            "reasons": ["Hostile demanding speech pattern in voicemail", "SYNTHETIC AUDIO"]
        })),
    )
}
