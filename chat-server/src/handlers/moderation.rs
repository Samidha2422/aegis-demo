use crate::models::SafetyObject;
use crate::state::AppState;
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use serde::{Deserialize, Serialize};

#[derive(Deserialize)]
pub struct ReportRequest {
    pub message_id: Option<String>,
    pub user_id: Option<String>,
    pub reason: String,
    pub details: Option<String>,
}

#[derive(Deserialize)]
pub struct NudgeRequest {
    pub draft: String,
}

#[derive(Serialize)]
pub struct NudgeResponse {
    pub severity: f32,
    pub tier: String,
    pub reasons: Vec<String>,
    pub recommended_action: String,
    pub nudge_text: Option<String>,
}

pub async fn block_user(State(state): State<AppState>, Path(id): Path<String>) -> StatusCode {
    {
        let mut blocked = state.blocked_users.write().await;
        blocked.insert(id.clone());
    }
    state
        .engine_client
        .report_refusal("meera", &id, "block")
        .await;
    StatusCode::OK
}

pub async fn unblock_user(State(state): State<AppState>, Path(id): Path<String>) -> StatusCode {
    let mut blocked = state.blocked_users.write().await;
    blocked.remove(&id);
    StatusCode::OK
}

pub async fn mute_user(State(state): State<AppState>, Path(id): Path<String>) -> StatusCode {
    let mut muted = state.muted_users.write().await;
    muted.insert(id);
    StatusCode::OK
}

pub async fn submit_report(
    State(_state): State<AppState>,
    Json(_payload): Json<ReportRequest>,
) -> StatusCode {
    // In demo, accept report and register
    StatusCode::CREATED
}

pub async fn check_nudge(
    State(state): State<AppState>,
    Json(payload): Json<NudgeRequest>,
) -> Json<NudgeResponse> {
    // Check via engine or fallback heuristic
    let engine_verdict = state.engine_client.score_draft(&payload.draft).await;

    if let Some(v) = engine_verdict {
        let nudge_text = if v.severity >= 50.0 {
            Some(
                "This message may be hurtful or toxic. Would you like to revise it before sending?"
                    .to_string(),
            )
        } else {
            None
        };
        Json(NudgeResponse {
            severity: v.severity,
            tier: v.tier,
            reasons: v.reasons,
            recommended_action: v.recommended_action,
            nudge_text,
        })
    } else {
        // Local heuristic fallback for common abusive phrases in drafts
        let lower = payload.draft.to_lowercase();
        let is_toxic =
            lower.contains("idiot") || lower.contains("pathetic") || lower.contains("hate you");
        let (severity, tier, nudge_text) = if is_toxic {
            (
                65.0,
                "review".to_string(),
                Some(
                    "This message may hurt someone. Would you like to edit it before sending?"
                        .to_string(),
                ),
            )
        } else {
            (0.0, "low".to_string(), None)
        };

        Json(NudgeResponse {
            severity,
            tier,
            reasons: if is_toxic {
                vec!["Draft contains potentially hostile language".to_string()]
            } else {
                Vec::new()
            },
            recommended_action: if is_toxic {
                "warn_sender".to_string()
            } else {
                "log".to_string()
            },
            nudge_text,
        })
    }
}
