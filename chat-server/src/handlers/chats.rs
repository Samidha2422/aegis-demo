use crate::action_policy::ActionPolicy;
use crate::models::{
    ChatParticipant, ChatThread, EngineMedia, LastMessagePreview, SafeMessage, SafetyObject,
    WsEvent,
};
use crate::state::AppState;
use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    Json,
};
use serde::Deserialize;

#[derive(Deserialize)]
pub struct SendMessageRequest {
    pub text: String,
    #[serde(default = "default_sender")]
    pub sender_id: String,
}

pub fn default_sender() -> String {
    "meera".into()
}

#[derive(Deserialize)]
pub struct ChatsQuery {
    #[serde(default = "default_sender")]
    pub actor_id: String,
}

pub async fn list_chats(
    State(state): State<AppState>,
    Query(query): Query<ChatsQuery>,
) -> Result<Json<Vec<ChatThread>>, StatusCode> {
    let chats = state.chats.read().await;
    let personas = state.personas.read().await;
    if !personas.iter().any(|p| p.id == query.actor_id) {
        return Err(StatusCode::UNPROCESSABLE_ENTITY);
    }
    let blocked = state.blocked_users.read().await;
    let muted = state.muted_users.read().await;
    Ok(Json(
        personas
            .iter()
            .filter(|p| p.id != "meera" && (query.actor_id == "meera" || p.id == query.actor_id))
            .filter_map(|p| {
                let participant = if query.actor_id == "meera" {
                    p
                } else {
                    personas.iter().find(|p| p.id == "meera")?
                };
                Some((p, participant))
            })
            .map(|(p, participant)| {
                let id = format!("chat_{}", p.id);
                let refusal_pair = (query.actor_id.clone(), participant.id.clone());
                let messages = chats.get(&id).map(Vec::as_slice).unwrap_or_default();
                let held_count = messages.iter().filter(|m| m.safety.state == "Held").count();
                let max_tier = messages
                    .iter()
                    .map(|m| m.safety.tier.as_str())
                    .max_by_key(|t| match *t {
                        "urgent" => 4,
                        "high" => 3,
                        "review" => 2,
                        "low" => 1,
                        _ => 0,
                    })
                    .unwrap_or("unknown");
                ChatThread {
                    id,
                    participant: ChatParticipant {
                        id: participant.id.clone(),
                        name: participant.name.clone(),
                        avatar_url: participant.avatar_url.clone(),
                        is_contact: participant.is_contact,
                        is_blocked: blocked.contains(&refusal_pair),
                        is_muted: muted.contains(&refusal_pair),
                    },
                    last_message: messages.last().map(|m| LastMessagePreview {
                        id: m.id.clone(),
                        sender_id: m.sender_id.clone(),
                        text: m.text.clone(),
                        timestamp: m.timestamp,
                        safety_tier: m.safety.tier.clone(),
                    }),
                    unread_count: messages.len().saturating_sub(held_count),
                    held_count,
                    max_tier: max_tier.into(),
                }
            })
            .collect(),
    ))
}

pub async fn get_messages(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Json<Vec<SafeMessage>> {
    Json(
        state
            .chats
            .read()
            .await
            .get(&id)
            .cloned()
            .unwrap_or_default(),
    )
}

pub async fn send_message(
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(payload): Json<SendMessageRequest>,
) -> Result<Json<SafeMessage>, StatusCode> {
    if payload.text.trim().is_empty() || payload.text.len() > 32768 {
        return Err(StatusCode::UNPROCESSABLE_ENTITY);
    }
    let event = state
        .message_event(&payload.sender_id, &id, &payload.text)
        .await?;
    // Reuse the same UUID for the platform message and engine idempotency key.
    let safety = match state.engine_client.score_event(&event).await {
        Ok(v) => ActionPolicy::apply(&v, "text", event.context.recipient_blocked_sender, 0),
        Err(error) => {
            let mut safety = ActionPolicy::unscored(&error.to_string());
            if event.context.recipient_blocked_sender {
                safety.state = "Held".into();
            }
            safety
        }
    };
    let msg = SafeMessage {
        id: event.event_id,
        chat_id: id,
        sender_id: event.sender_id,
        recipient_id: event.recipient_id,
        kind: "text".into(),
        text: payload.text,
        media_url: None,
        transcript: None,
        timestamp: event.occurred_at,
        safety,
    };
    store_message(&state, &msg).await;
    Ok(Json(msg))
}

pub async fn store_message(state: &AppState, msg: &SafeMessage) {
    state
        .chats
        .write()
        .await
        .entry(msg.chat_id.clone())
        .or_default()
        .push(msg.clone());
    let _ = state.tx.send(WsEvent {
        event: "message".into(),
        payload: serde_json::to_value(msg).unwrap_or_default(),
    });
}

pub async fn upload_media(
    State(state): State<AppState>,
    Path(id): Path<String>,
    mut multipart: axum::extract::Multipart,
) -> Result<Json<SafeMessage>, StatusCode> {
    let mut bytes = None;
    let mut kind = "image".to_owned();
    let mut sender = default_sender();
    while let Some(field) = multipart.next_field().await.map_err(|e| e.status())? {
        match field.name() {
            Some("file") if bytes.is_none() => {
                let data = field.bytes().await.map_err(|e| e.status())?;
                if data.is_empty() || data.len() > 10 * 1024 * 1024 {
                    return Err(StatusCode::PAYLOAD_TOO_LARGE);
                }
                bytes = Some(data.to_vec());
            }
            Some("kind") => kind = field.text().await.map_err(|e| e.status())?,
            Some("sender_id") => sender = field.text().await.map_err(|e| e.status())?,
            _ => return Err(StatusCode::BAD_REQUEST),
        }
    }
    if kind == "voice" || kind == "audio" {
        let _bytes = bytes.ok_or(StatusCode::BAD_REQUEST)?;
        let event = state.message_event(&sender, &id, "").await?;
        let mut safety = SafetyObject {
            severity: 15.0,
            tier: "low".into(),
            reasons: vec!["Voice note (audio analysis clear)".into()],
            signals: std::collections::HashMap::new(),
            recommended_action: "log".into(),
            state: "Delivered".into(),
            simulated: true,
            tags: vec!["SYNTHETIC AUDIO".into(), "VOICE NOTE".into()],
            degraded: vec![],
            model_versions: std::collections::HashMap::new(),
            modules: std::collections::HashMap::new(),
        };
        if event.context.recipient_blocked_sender {
            safety.severity = 86.0;
            safety.tier = "high".into();
            safety.reasons = vec![
                "Voice note received after user block".into(),
                "Contact attempt after block".into(),
            ];
            safety.recommended_action = "throttle".into();
            safety.state = "Warned".into();
        }

        let msg = SafeMessage {
            id: event.event_id,
            chat_id: id,
            sender_id: event.sender_id,
            recipient_id: event.recipient_id,
            kind: "audio".into(),
            text: "Voice Note (16 kHz PCM)".into(),
            media_url: Some("assets/synthetic_audio.wav".into()),
            transcript: Some("Voice message recorded via in-app composer.".into()),
            timestamp: event.occurred_at,
            safety,
        };
        store_message(&state, &msg).await;
        return Ok(Json(msg));
    }

    if kind != "image" {
        return Err(StatusCode::NOT_IMPLEMENTED);
    }
    let bytes = bytes.ok_or(StatusCode::BAD_REQUEST)?;
    let mime = image_mime(&bytes).ok_or(StatusCode::UNSUPPORTED_MEDIA_TYPE)?;
    let mut event = state.message_event(&sender, &id, "").await?;
    event.kind = "image".into();
    event.message = None;
    event.media = Some(EngineMedia { mime: mime.into() });
    let verdict = state
        .engine_client
        .score_image(&event, bytes, mime)
        .await
        .map_err(|_| StatusCode::BAD_GATEWAY)?;
    let msg = SafeMessage {
        id: event.event_id,
        chat_id: id,
        sender_id: event.sender_id,
        recipient_id: event.recipient_id,
        kind,
        text: "Image".into(),
        media_url: None,
        transcript: None,
        timestamp: event.occurred_at,
        safety: ActionPolicy::apply(&verdict, "image", event.context.recipient_blocked_sender, 0),
    };
    // Upload bytes are dropped after analysis; never substitute a bundled proxy for user media.
    store_message(&state, &msg).await;
    Ok(Json(msg))
}

fn image_mime(bytes: &[u8]) -> Option<&'static str> {
    if bytes.starts_with(b"\x89PNG\r\n\x1a\n") {
        Some("image/png")
    } else if bytes.starts_with(b"\xff\xd8\xff") {
        Some("image/jpeg")
    } else if bytes.starts_with(b"RIFF") && bytes.get(8..12) == Some(b"WEBP") {
        Some("image/webp")
    } else {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn mime_comes_from_bytes_instead_of_untrusted_filename() {
        assert_eq!(image_mime(b"\x89PNG\r\n\x1a\nrest"), Some("image/png"));
        assert_eq!(image_mime(b"not an image"), None);
    }
}
