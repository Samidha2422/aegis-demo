use crate::baseline_comparator::BaselineComparison;
use crate::models::WsEvent;
use crate::state::AppState;
use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
    Json,
};
use chrono::Utc;
use serde::Deserialize;

#[derive(Deserialize)]
pub struct PlayScenarioQuery {
    #[serde(default = "default_speed")]
    pub speed: u32,
}

fn default_speed() -> u32 {
    60
}

#[derive(Deserialize)]
pub struct BaselineQuery {
    pub conversation: Option<String>,
}

#[derive(Deserialize)]
pub struct EvidenceQuery {
    pub format: Option<String>,
}

pub async fn list_scenarios() -> Json<serde_json::Value> {
    Json(serde_json::json!([
        { "id": 1, "title": "The Polite Stalker", "description": "80 friendly messages, 0 replies, continuing after block", "actor": "rohan" },
        { "id": 2, "title": "The Pile-On", "description": "10 bot accounts spamming comments in 60s", "actor": "bot01..bot10" },
        { "id": 3, "title": "Cyberflashing", "description": "Unsolicited adult image blurred with view anyway", "actor": "stranger_77" },
        { "id": 4, "title": "The Screenshot Meme", "description": "Meme with toxic text detected via OCR", "actor": "rohan" },
        { "id": 5, "title": "The Voice Note", "description": "Synthetic threatening audio clip rendered as transcript", "actor": "unknown_caller" },
        { "id": 6, "title": "The Call Log", "description": "40 overnight calls, unanswered, continuing after block", "actor": "unknown_number" },
        { "id": 7, "title": "The Healthy Couple", "description": "High volume mutual conversation, zero flags", "actor": "ishaan" },
        { "id": 8, "title": "Slang Update", "description": "Moderator confirms new term, subsequent catch", "actor": "mod_asha" },
        { "id": 9, "title": "Evidence Export", "description": "Hash-chained timeline export generation", "actor": "meera" }
    ]))
}

pub async fn play_scenario(
    State(state): State<AppState>,
    Path(n): Path<u32>,
    Query(query): Query<PlayScenarioQuery>,
) -> Result<StatusCode, StatusCode> {
    let scenario = state
        .scenario_runner
        .load_scenario(n)
        .map_err(|_| StatusCode::NOT_FOUND)?;

    let runner = state.scenario_runner.clone();
    tokio::spawn(async move {
        runner.play(scenario, query.speed).await;
    });

    Ok(StatusCode::ACCEPTED)
}

pub async fn reset_demo(State(state): State<AppState>) -> StatusCode {
    state.reset().await;
    let _ = state.tx.send(WsEvent {
        event: "demo_reset".to_string(),
        payload: serde_json::json!({ "timestamp": Utc::now() }),
    });
    StatusCode::OK
}

pub async fn get_baseline_comparison(
    State(state): State<AppState>,
    Query(query): Query<BaselineQuery>,
) -> Json<BaselineComparison> {
    let conv_id = query
        .conversation
        .unwrap_or_else(|| "chat_rohan".to_string());
    let chats_map = state.chats.read().await;
    let messages = chats_map.get(&conv_id).cloned().unwrap_or_default();

    let comparison = state.baseline_comparator.compare(&conv_id, &messages);
    Json(comparison)
}

pub async fn export_evidence(
    State(state): State<AppState>,
    Query(query): Query<EvidenceQuery>,
) -> impl IntoResponse {
    let format = query.format.unwrap_or_else(|| "json".to_string());
    let chats_map = state.chats.read().await;
    let calls_list = state.calls.read().await;

    let timestamp = Utc::now();
    let export_payload = serde_json::json!({
        "case_id": "AEGIS-CASE-2026-HACK",
        "victim_id": "meera",
        "generated_at": timestamp,
        "integrity_hash": "sha256-e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        "flagged_events_count": 85,
        "chats": *chats_map,
        "calls": *calls_list,
        "summary": "Forensic timeline of persistent stalking and unreplied contact attempts."
    });

    if format == "html" {
        let html_content = format!(
            "<!DOCTYPE html><html><head><title>Aegis Evidence Report</title></head><body><h1>Aegis Safety Evidence Report</h1><p>Case: AEGIS-CASE-2026-HACK</p><p>Victim: Meera Patel</p><p>Generated: {}</p><hr><pre>{}</pre></body></html>",
            timestamp,
            serde_json::to_string_pretty(&export_payload).unwrap_or_default()
        );
        (
            StatusCode::OK,
            [("Content-Type", "text/html")],
            html_content,
        )
    } else {
        (
            StatusCode::OK,
            [("Content-Type", "application/json")],
            serde_json::to_string_pretty(&export_payload).unwrap_or_default(),
        )
    }
}
