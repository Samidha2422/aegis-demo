use crate::state::AppState;
use axum::{
    extract::{
        ws::{Message, WebSocket},
        State, WebSocketUpgrade,
    },
    response::IntoResponse,
};
use tracing::{info, warn};

pub async fn ws_handler(ws: WebSocketUpgrade, State(state): State<AppState>) -> impl IntoResponse {
    ws.on_upgrade(|socket| handle_socket(socket, state))
}

async fn handle_socket(mut socket: WebSocket, state: AppState) {
    info!("New WebSocket connection established");
    let mut rx = state.tx.subscribe();

    loop {
        tokio::select! {
            Ok(event) = rx.recv() => {
                if let Ok(msg_text) = serde_json::to_string(&event) {
                    if socket.send(Message::Text(msg_text)).await.is_err() {
                        warn!("WebSocket client disconnected");
                        break;
                    }
                }
            }
            msg = socket.recv() => {
                match msg {
                    Some(Ok(Message::Close(_))) | None => {
                        info!("WebSocket connection closed gracefully");
                        break;
                    }
                    _ => {}
                }
            }
        }
    }
}
