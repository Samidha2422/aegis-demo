use crate::baseline_comparator::BaselineComparator;
use crate::engine_client::EngineClient;
use crate::models::{
    AlertItem, CallRecord, EngineEventEnvelope, Persona, Post, SafeMessage, WsEvent,
};
use crate::scenario_runner::ScenarioRunner;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use tokio::sync::{broadcast, RwLock};

#[derive(Clone)]
pub struct AppState {
    pub personas: Arc<RwLock<Vec<Persona>>>,
    pub chats: Arc<RwLock<HashMap<String, Vec<SafeMessage>>>>,
    pub blocked_users: Arc<RwLock<HashSet<(String, String)>>>,
    pub muted_users: Arc<RwLock<HashSet<(String, String)>>>,
    pub posts: Arc<RwLock<Vec<Post>>>,
    pub calls: Arc<RwLock<Vec<CallRecord>>>,
    pub alerts: Arc<RwLock<Vec<AlertItem>>>,
    pub engine_client: Arc<EngineClient>,
    pub baseline_comparator: Arc<BaselineComparator>,
    pub scenario_runner: Arc<ScenarioRunner>,
    pub tx: broadcast::Sender<WsEvent>,
}

impl AppState {
    /// Build direction-aware context; Meera blocking Rohan never means Rohan blocked Meera.
    pub async fn message_event(
        &self,
        sender: &str,
        chat_id: &str,
        text: &str,
    ) -> Result<EngineEventEnvelope, axum::http::StatusCode> {
        let peer = chat_id
            .strip_prefix("chat_")
            .ok_or(axum::http::StatusCode::UNPROCESSABLE_ENTITY)?;
        let personas = self.personas.read().await;
        let sender_persona = personas
            .iter()
            .find(|p| p.id == sender)
            .ok_or(axum::http::StatusCode::UNPROCESSABLE_ENTITY)?;
        let peer_persona = personas
            .iter()
            .find(|p| p.id == peer)
            .ok_or(axum::http::StatusCode::NOT_FOUND)?;
        if sender != "meera" && sender != peer {
            return Err(axum::http::StatusCode::UNPROCESSABLE_ENTITY);
        }
        let recipient = if sender == "meera" { peer } else { "meera" };
        let mut event = EngineEventEnvelope::message(sender, recipient, chat_id, text);
        event.context.is_contact = if recipient == "meera" {
            sender_persona.is_contact
        } else {
            peer_persona.is_contact
        };
        let refusal_pair = (recipient.to_owned(), sender.to_owned());
        event.context.recipient_blocked_sender =
            self.blocked_users.read().await.contains(&refusal_pair);
        event.context.recipient_muted_sender =
            self.muted_users.read().await.contains(&refusal_pair);
        event.context.sender_account_age_days = 365;
        Ok(event)
    }

    pub fn new(engine_url: String) -> Self {
        let (tx, _) = broadcast::channel(100);
        let personas = vec![
            Persona {
                id: "meera".to_string(),
                name: "Meera Patel".to_string(),
                avatar_url: "assets/avatars/meera.png".to_string(),
                role: "victim".to_string(),
                is_contact: true,
                bio: "Designer & active social user".to_string(),
            },
            Persona {
                id: "rohan".to_string(),
                name: "Rohan Sharma".to_string(),
                avatar_url: "assets/avatars/rohan.png".to_string(),
                role: "stalker".to_string(),
                is_contact: false,
                bio: "Persistent acquaintance".to_string(),
            },
            Persona {
                id: "ishaan".to_string(),
                name: "Ishaan Verma".to_string(),
                avatar_url: "assets/avatars/ishaan.png".to_string(),
                role: "partner".to_string(),
                is_contact: true,
                bio: "Meera's partner".to_string(),
            },
            Persona {
                id: "diya".to_string(),
                name: "Diya Sen".to_string(),
                avatar_url: "assets/avatars/diya.png".to_string(),
                role: "partner".to_string(),
                is_contact: true,
                bio: "Friend & colleague".to_string(),
            },
            Persona {
                id: "stranger_77".to_string(),
                name: "Unknown User (77)".to_string(),
                avatar_url: "assets/avatars/unknown.png".to_string(),
                role: "cyberflasher".to_string(),
                is_contact: false,
                bio: "".to_string(),
            },
            Persona {
                id: "mod_asha".to_string(),
                name: "Asha (Moderator)".to_string(),
                avatar_url: "assets/avatars/mod.png".to_string(),
                role: "moderator".to_string(),
                is_contact: true,
                bio: "Safety operations team".to_string(),
            },
        ];

        let engine_client = Arc::new(EngineClient::new(engine_url));
        let baseline_comparator = Arc::new(BaselineComparator::new());
        let scenario_runner = Arc::new(ScenarioRunner::new(tx.clone()));

        Self {
            personas: Arc::new(RwLock::new(personas)),
            chats: Arc::new(RwLock::new(HashMap::new())),
            blocked_users: Arc::new(RwLock::new(HashSet::new())),
            muted_users: Arc::new(RwLock::new(HashSet::new())),
            posts: Arc::new(RwLock::new(Vec::new())),
            calls: Arc::new(RwLock::new(Vec::new())),
            alerts: Arc::new(RwLock::new(Vec::new())),
            engine_client,
            baseline_comparator,
            scenario_runner,
            tx,
        }
    }

    /// Resets all ephemeral state back to initial state
    pub async fn reset(&self) {
        let mut chats = self.chats.write().await;
        chats.clear();

        let mut blocked = self.blocked_users.write().await;
        blocked.clear();

        let mut muted = self.muted_users.write().await;
        muted.clear();

        let mut posts = self.posts.write().await;
        posts.clear();

        let mut calls = self.calls.write().await;
        calls.clear();

        let mut alerts = self.alerts.write().await;
        alerts.clear();
    }
}
