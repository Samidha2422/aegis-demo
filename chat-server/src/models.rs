use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Persona {
    pub id: String,
    pub name: String,
    pub avatar_url: String,
    pub role: String,
    pub is_contact: bool,
    pub bio: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatThread {
    pub id: String,
    pub participant: ChatParticipant,
    pub last_message: Option<LastMessagePreview>,
    pub unread_count: usize,
    pub held_count: usize,
    pub max_tier: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatParticipant {
    pub id: String,
    pub name: String,
    pub avatar_url: String,
    pub is_contact: bool,
    pub is_blocked: bool,
    pub is_muted: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LastMessagePreview {
    pub id: String,
    pub sender_id: String,
    pub text: String,
    pub timestamp: DateTime<Utc>,
    pub safety_tier: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SafeMessage {
    pub id: String,
    pub chat_id: String,
    pub sender_id: String,
    pub recipient_id: String,
    pub kind: String, // "text", "image", "voice"
    pub text: String,
    pub media_url: Option<String>,
    pub transcript: Option<String>,
    pub timestamp: DateTime<Utc>,
    pub safety: SafetyObject,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SafetyObject {
    pub severity: f32, // 0.0 - 100.0
    pub tier: String,  // "low", "review", "high", "urgent"
    pub reasons: Vec<String>,
    pub signals: HashMap<String, f32>,
    pub recommended_action: String, // "log", "warn_sender", "blur_media", "throttle", "escalate"
    pub state: String, // "Delivered", "Scanning", "Warned", "Blurred", "Hidden", "Held", "Escalated", "TranscriptOnly", "EngineUnavailable"
    pub simulated: bool,
    pub tags: Vec<String>,
}

impl Default for SafetyObject {
    fn default() -> Self {
        Self {
            severity: 0.0,
            tier: "low".to_string(),
            reasons: Vec::new(),
            signals: HashMap::new(),
            recommended_action: "log".to_string(),
            state: "Delivered".to_string(),
            simulated: false,
            tags: Vec::new(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EngineVerdict {
    pub severity: f32,
    pub tier: String,
    pub reasons: Vec<String>,
    pub signals: HashMap<String, f32>,
    pub recommended_action: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EngineEventEnvelope {
    pub event_id: String,
    pub kind: String,
    pub occurred_at: DateTime<Utc>,
    pub sender_id: String,
    pub recipient_id: String,
    pub platform: String,
    pub channel: String,
    pub context: EventContext,
    pub content: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EventContext {
    pub is_contact: bool,
    pub recipient_blocked_sender: bool,
    pub stop_requested: bool,
    pub sender_account_age_days: i32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reply_to_event_id: Option<String>,
    #[serde(default)]
    pub mentions: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Post {
    pub id: String,
    pub author_id: String,
    pub content: String,
    pub timestamp: DateTime<Utc>,
    pub comments_count: usize,
    pub is_protected: bool,
    pub comments: Vec<Comment>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Comment {
    pub id: String,
    pub post_id: String,
    pub author_id: String,
    pub content: String,
    pub timestamp: DateTime<Utc>,
    pub safety: SafetyObject,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CallRecord {
    pub id: String,
    pub caller_id: String,
    pub callee_id: String,
    pub timestamp: DateTime<Utc>,
    pub duration_secs: u32,
    pub status: String, // "answered", "missed", "rejected"
    pub safety: SafetyObject,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AlertItem {
    pub id: String,
    pub tier: String,
    pub title: String,
    pub description: String,
    pub target_user_id: String,
    pub timestamp: DateTime<Utc>,
    pub reasons: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WsEvent {
    pub event: String, // "message", "safety_update", "alert", "call_event", "scenario_step"
    pub payload: serde_json::Value,
}
