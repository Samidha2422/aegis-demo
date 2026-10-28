use crate::models::Persona;
use crate::state::AppState;
use axum::{extract::State, Json};
use serde::{Deserialize, Serialize};

#[derive(Deserialize)]
pub struct LoginRequest {
    pub persona_id: String,
}

#[derive(Serialize)]
pub struct LoginResponse {
    pub token: String,
    pub persona: Persona,
}

pub async fn list_personas(State(state): State<AppState>) -> Json<Vec<Persona>> {
    let personas = state.personas.read().await;
    Json(personas.clone())
}

pub async fn login(
    State(state): State<AppState>,
    Json(payload): Json<LoginRequest>,
) -> Result<Json<LoginResponse>, axum::http::StatusCode> {
    let personas = state.personas.read().await;
    let persona = personas
        .iter()
        .find(|p| p.id == payload.persona_id)
        .cloned()
        .ok_or(axum::http::StatusCode::NOT_FOUND)?;

    let token = format!("bearer_{}_session_token", persona.id);
    Ok(Json(LoginResponse { token, persona }))
}
