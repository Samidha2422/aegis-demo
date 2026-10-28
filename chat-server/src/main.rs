use axum::{
    routing::{get, post},
    Router,
};
use std::net::SocketAddr;
use tower_http::cors::{Any, CorsLayer};
use tracing::info;

mod action_policy;
mod baseline_comparator;
mod engine_client;
mod handlers;
mod models;
mod scenario_runner;
mod state;

use state::AppState;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Initialize tracing
    tracing_subscriber::fmt()
        .with_env_filter("chat_server=info,tower_http=info")
        .init();

    let engine_url =
        std::env::var("AEGIS_ENGINE_URL").unwrap_or_else(|_| "http://127.0.0.1:8080".to_string());
    let state = AppState::new(engine_url);

    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    let state_seed = state.clone();
    tokio::spawn(async move {
        state_seed.seed_engine_history().await;
    });

    let app = Router::new()
        .route("/engine/status", get(handlers::moderation::engine_status))
        // Auth & Personas
        .route("/personas", get(handlers::auth::list_personas))
        .route("/auth/login", post(handlers::auth::login))
        // Chats & Messaging
        .route("/chats", get(handlers::chats::list_chats))
        .route(
            "/chats/:id/messages",
            get(handlers::chats::get_messages).post(handlers::chats::send_message),
        )
        .route("/chats/:id/media", post(handlers::chats::upload_media))
        // Moderation & Nudge
        .route("/users/:id/block", post(handlers::moderation::block_user))
        .route(
            "/users/:id/unblock",
            post(handlers::moderation::unblock_user),
        )
        .route("/users/:id/mute", post(handlers::moderation::mute_user))
        .route("/reports", post(handlers::moderation::submit_report))
        .route("/nudge", post(handlers::moderation::check_nudge))
        // Feed (Comments / Pile-on)
        .route("/feed/posts", get(handlers::feed::list_posts))
        .route(
            "/feed/posts/:id/comments",
            post(handlers::feed::add_comment),
        )
        // Calls & Recordings
        .route("/calls/log", get(handlers::calls::list_calls))
        .route("/calls/simulate", post(handlers::calls::simulate_call))
        .route("/calls/log/import", post(handlers::calls::import_call_log))
        .route("/recordings", post(handlers::calls::upload_recording))
        // Demo Orchestrator & Evidence
        .route("/demo/scenarios", get(handlers::demo::list_scenarios))
        .route(
            "/demo/scenarios/:n/play",
            post(handlers::demo::play_scenario),
        )
        .route("/demo/reset", post(handlers::demo::reset_demo))
        .route(
            "/demo/baseline",
            get(handlers::demo::get_baseline_comparison),
        )
        .route("/evidence/export", get(handlers::demo::export_evidence))
        // Realtime WebSocket
        .route("/ws", get(handlers::ws::ws_handler))
        .layer(axum::extract::DefaultBodyLimit::max(
            10 * 1024 * 1024 + 65536,
        ))
        .layer(cors)
        .with_state(state);

    let port: u16 = std::env::var("PORT")
        .unwrap_or_else(|_| "8081".to_string())
        .parse()
        .unwrap_or(8081);

    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    info!("Aegis Chat Server listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}
