use crate::action_policy::ActionPolicy;
use crate::models::{
    ChatParticipant, ChatThread, EngineEventEnvelope, EventContext, LastMessagePreview,
    SafeMessage, WsEvent,
};
use crate::state::AppState;
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use chrono::Utc;
use serde::Deserialize;
use std::collections::HashMap;
use uuid::Uuid;

#[derive(Deserialize)]
pub struct SendMessageRequest {
    pub text: String,
}

pub async fn list_chats(State(state): State<AppState>) -> Json<Vec<ChatThread>> {
    let chats_map = state.chats.read().await;
    let personas = state.personas.read().await;
    let blocked_set = state.blocked_users.read().await;
    let muted_set = state.muted_users.read().await;

    let mut threads = Vec::new();

    for persona in personas.iter() {
        if persona.id == "meera" {
            continue; // Skip the default owner
        }

        let chat_id = format!("chat_{}", persona.id);
        let messages = chats_map.get(&chat_id).cloned().unwrap_or_default();

        let is_blocked = blocked_set.contains(&persona.id);
        let is_muted = muted_set.contains(&persona.id);

        let last_message = messages.last().map(|m| LastMessagePreview {
            id: m.id.clone(),
            sender_id: m.sender_id.clone(),
            text: m.text.clone(),
            timestamp: m.timestamp,
            safety_tier: m.safety.tier.clone(),
        });

        let held_count = messages.iter().filter(|m| m.safety.state == "Held").count();
        let unread_count = messages.len().saturating_sub(held_count);

        let max_tier = messages
            .iter()
            .map(|m| m.safety.tier.as_str())
            .max_by_key(|t| match *t {
                "urgent" => 4,
                "high" => 3,
                "review" => 2,
                _ => 1,
            })
            .unwrap_or("low")
            .to_string();

        threads.push(ChatThread {
            id: chat_id,
            participant: ChatParticipant {
                id: persona.id.clone(),
                name: persona.name.clone(),
                avatar_url: persona.avatar_url.clone(),
                is_contact: persona.is_contact,
                is_blocked,
                is_muted,
            },
            last_message,
            unread_count,
            held_count,
            max_tier,
        });
    }

    Json(threads)
}

pub async fn get_messages(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Json<Vec<SafeMessage>> {
    let chats_map = state.chats.read().await;
    let messages = chats_map.get(&id).cloned().unwrap_or_default();
    Json(messages)
}

pub async fn send_message(
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(payload): Json<SendMessageRequest>,
) -> Result<Json<SafeMessage>, StatusCode> {
    let sender_id = "meera".to_string(); // Default owner
    let recipient_id = id.strip_prefix("chat_").unwrap_or(&id).to_string();
    let msg_id = format!("msg_{}", Uuid::new_v4().to_string().replace('-', ""));

    let is_blocked = {
        let blocked = state.blocked_users.read().await;
        blocked.contains(&recipient_id)
    };

    let occurred_at = Utc::now();

    // Construct Engine Event Envelope
    let mut content = HashMap::new();
    content.insert("text".to_string(), serde_json::json!(payload.text));

    let envelope = EngineEventEnvelope {
        event_id: msg_id.clone(),
        kind: "message".to_string(),
        occurred_at,
        sender_id: sender_id.clone(),
        recipient_id: recipient_id.clone(),
        platform: "demo-chat".to_string(),
        channel: "dm".to_string(),
        context: EventContext {
            is_contact: true,
            recipient_blocked_sender: is_blocked,
            stop_requested: false,
            sender_account_age_days: 365,
            reply_to_event_id: None,
            mentions: Vec::new(),
        },
        content,
    };

    // Call Engine with fallback
    let verdict_opt = state.engine_client.score_event(&envelope).await;
    let safety = match verdict_opt {
        Some(ref v) => ActionPolicy::apply(v, "text", is_blocked, 0),
        None => ActionPolicy::fallback_unscored(),
    };

    let msg = SafeMessage {
        id: msg_id,
        chat_id: id.clone(),
        sender_id,
        recipient_id,
        kind: "text".to_string(),
        text: payload.text,
        media_url: None,
        transcript: None,
        timestamp: occurred_at,
        safety,
    };

    {
        let mut chats = state.chats.write().await;
        chats.entry(id).or_default().push(msg.clone());
    }

    // Broadcast WebSocket message event
    let _ = state.tx.send(WsEvent {
        event: "message".to_string(),
        payload: serde_json::to_value(&msg).unwrap_or_default(),
    });

    Ok(Json(msg))
}

pub async fn upload_media(
    State(state): State<AppState>,
    Path(id): Path<String>,
    mut multipart: axum::extract::Multipart,
) -> Result<Json<SafeMessage>, StatusCode> {
    let mut file_name = "upload.jpg".to_string();
    let mut kind = "image".to_string();

    while let Ok(Some(field)) = multipart.next_field().await {
        let name = field.name().unwrap_or("").to_string();
        if name == "file" {
            if let Some(fname) = field.file_name() {
                file_name = fname.to_string();
            }
            let _ = field.bytes().await;
        } else if name == "kind" {
            if let Ok(text) = field.text().await {
                kind = text;
            }
        }
    }

    let sender_id = "meera".to_string();
    let recipient_id = id.strip_prefix("chat_").unwrap_or(&id).to_string();
    let msg_id = format!("msg_{}", Uuid::new_v4().to_string().replace('-', ""));
    let occurred_at = Utc::now();

    let is_blocked = {
        let blocked = state.blocked_users.read().await;
        blocked.contains(&recipient_id)
    };

    let is_contact = !id.contains("stranger") && !id.contains("unknown");

    // Evaluate image upload rules (handle blur_media, display risk reasons & degradation status)
    let safety = if !is_contact || is_blocked {
        crate::models::SafetyObject {
            severity: 82.0,
            tier: "high".to_string(),
            reasons: vec![
                "Unsolicited media from non-contact".to_string(),
                "NSFW inference connecting (rule degradation active)".to_string(),
            ],
            signals: HashMap::from([
                ("heuristic_risk".to_string(), 0.82),
                ("nsfw_model_status".to_string(), 0.0),
            ]),
            recommended_action: "blur_media".to_string(),
            state: "Blurred".to_string(),
            simulated: true,
            tags: vec![
                "DEMO PROXY IMAGE".to_string(),
                "SIMULATED".to_string(),
                "NSFW_INFERENCE_CONNECTING".to_string(),
            ],
        }
    } else {
        crate::models::SafetyObject {
            severity: 12.0,
            tier: "low".to_string(),
            reasons: vec![
                "Media shared within active contact thread".to_string(),
                "NSFW inference connecting (clean heuristic)".to_string(),
            ],
            signals: HashMap::from([("heuristic_risk".to_string(), 0.12)]),
            recommended_action: "log".to_string(),
            state: "Delivered".to_string(),
            simulated: true,
            tags: vec!["DEMO PROXY IMAGE".to_string(), "SIMULATED".to_string()],
        }
    };

    let msg = SafeMessage {
        id: msg_id,
        chat_id: id.clone(),
        sender_id,
        recipient_id,
        kind,
        text: file_name,
        media_url: Some("assets/proxy_nsfw.png".to_string()),
        transcript: None,
        timestamp: occurred_at,
        safety,
    };

    {
        let mut chats = state.chats.write().await;
        chats.entry(id).or_default().push(msg.clone());
    }

    let _ = state.tx.send(WsEvent {
        event: "message".to_string(),
        payload: serde_json::to_value(&msg).unwrap_or_default(),
    });

    Ok(Json(msg))
}
