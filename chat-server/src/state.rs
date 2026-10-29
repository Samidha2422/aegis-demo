use crate::baseline_comparator::BaselineComparator;
use crate::engine_client::EngineClient;
use crate::models::{
    AlertItem, CallRecord, EngineEventEnvelope, Persona, Post, SafeMessage, SafetyObject, WsEvent,
};
use crate::scenario_runner::ScenarioRunner;
use chrono::Utc;
use std::collections::{HashMap, HashSet};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use tokio::sync::{broadcast, RwLock};

type MediaStore = HashMap<String, (&'static str, Vec<u8>)>;

#[derive(Clone)]
pub struct AppState {
    pub personas: Arc<RwLock<Vec<Persona>>>,
    pub chats: Arc<RwLock<HashMap<String, Vec<SafeMessage>>>>,
    pub media: Arc<RwLock<MediaStore>>,
    pub media_generation: Arc<AtomicU64>,
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
            chats: Arc::new(RwLock::new(Self::initial_chats())),
            media: Arc::new(RwLock::new(HashMap::new())),
            media_generation: Arc::new(AtomicU64::new(0)),
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

    pub fn initial_chats() -> HashMap<String, Vec<SafeMessage>> {
        let now = Utc::now();
        let mut map = HashMap::new();

        // Ishaan (Meera's partner / healthy reciprocal relationship)
        let ishaan_msgs = vec![
            (
                "meera",
                "ishaan",
                "Morning! Are you heading to the office soon?",
                120,
            ),
            (
                "ishaan",
                "meera",
                "Leaving in 10 mins. Want me to grab coffee?",
                115,
            ),
            (
                "meera",
                "ishaan",
                "Oat latte from the corner cafe please!",
                110,
            ),
            ("ishaan", "meera", "You got it! See you at 10.", 105),
            ("meera", "ishaan", "Thanks so much, you're the best!", 90),
            (
                "ishaan",
                "meera",
                "Haha of course! How's the user research doc?",
                75,
            ),
            (
                "meera",
                "ishaan",
                "Almost done, findings look really positive.",
                60,
            ),
            (
                "ishaan",
                "meera",
                "Nice! Still good for dinner tonight?",
                45,
            ),
            ("meera", "ishaan", "Yes! 8pm at the Italian place?", 30),
            (
                "ishaan",
                "meera",
                "Perfect, table is already booked. See you tonight!",
                15,
            ),
        ];

        let ishaan_vec = ishaan_msgs
            .into_iter()
            .enumerate()
            .map(|(idx, (snd, rcv, text, mins_ago))| SafeMessage {
                id: format!("msg_seed_ishaan_{idx}"),
                chat_id: "chat_ishaan".into(),
                sender_id: snd.into(),
                recipient_id: rcv.into(),
                kind: "text".into(),
                text: text.into(),
                media_url: None,
                transcript: None,
                timestamp: now - chrono::Duration::minutes(mins_ago),
                safety: SafetyObject {
                    severity: 0.0,
                    tier: "low".into(),
                    reasons: vec![],
                    signals: HashMap::new(),
                    recommended_action: "allow".into(),
                    secondary_actions: Vec::new(),
                    state: "Delivered".into(),
                    simulated: false,
                    tags: vec![],
                    degraded: vec![],
                    model_versions: HashMap::new(),
                    modules: HashMap::new(),
                },
            })
            .collect();

        // Diya (Close friend & colleague)
        let diya_msgs = vec![
            (
                "diya",
                "meera",
                "Meera!! Did you see the research acceptance email?!",
                180,
            ),
            (
                "meera",
                "diya",
                "OMG YES! I couldn't believe it when I saw it!",
                175,
            ),
            (
                "diya",
                "meera",
                "So well deserved!! The design system research was incredible.",
                160,
            ),
            (
                "meera",
                "diya",
                "Couldn't have done it without your data help, honestly.",
                145,
            ),
            (
                "diya",
                "meera",
                "Team effort! We have to celebrate this weekend.",
                120,
            ),
            ("meera", "diya", "Definitely! Saturday brunch?", 90),
            (
                "diya",
                "meera",
                "Saturday 11 AM at our spot. Can't wait!",
                60,
            ),
        ];

        let diya_vec = diya_msgs
            .into_iter()
            .enumerate()
            .map(|(idx, (snd, rcv, text, mins_ago))| SafeMessage {
                id: format!("msg_seed_diya_{idx}"),
                chat_id: "chat_diya".into(),
                sender_id: snd.into(),
                recipient_id: rcv.into(),
                kind: "text".into(),
                text: text.into(),
                media_url: None,
                transcript: None,
                timestamp: now - chrono::Duration::minutes(mins_ago),
                safety: SafetyObject {
                    severity: 0.0,
                    tier: "low".into(),
                    reasons: vec![],
                    signals: HashMap::new(),
                    recommended_action: "allow".into(),
                    secondary_actions: Vec::new(),
                    state: "Delivered".into(),
                    simulated: false,
                    tags: vec![],
                    degraded: vec![],
                    model_versions: HashMap::new(),
                    modules: HashMap::new(),
                },
            })
            .collect();

        map.insert("chat_ishaan".into(), ishaan_vec);
        map.insert("chat_diya".into(), diya_vec);
        map
    }

    pub async fn seed_engine_history(&self) -> Result<(), crate::engine_client::EngineError> {
        let chats = self.chats.read().await.clone();
        for (chat_id, msgs) in &chats {
            for m in msgs {
                let mut event =
                    EngineEventEnvelope::message(&m.sender_id, &m.recipient_id, chat_id, &m.text);
                event.occurred_at = m.timestamp;
                event.draft = false;
                event.context.is_contact = true;
                self.engine_client.score_event(&event).await?;
            }
        }
        Ok(())
    }

    /// Resets all ephemeral state back to initial state
    pub async fn reset(&self) {
        let mut chats = self.chats.write().await;
        self.media_generation.fetch_add(1, Ordering::SeqCst);
        *chats = Self::initial_chats();
        self.media.write().await.clear();

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

        for target in ["rohan", "stranger_77", "ishaan", "diya"] {
            let _ = self
                .engine_client
                .report_refusal("meera", target, "unblock")
                .await;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::{extract::State, routing::post, Json, Router};
    use tokio::sync::Mutex;

    #[tokio::test]
    async fn seed_history_finishes_all_messages_before_returning() {
        async fn record(
            State(events): State<Arc<Mutex<Vec<EngineEventEnvelope>>>>,
            Json(event): Json<EngineEventEnvelope>,
        ) -> Json<serde_json::Value> {
            events.lock().await.push(event);
            Json(serde_json::json!({
                "severity": 0, "tier": "low", "reasons": [], "signals": {},
                "recommended_action": "allow", "simulated": false,
                "degraded": [], "model_versions": {}
            }))
        }

        let events = Arc::new(Mutex::new(Vec::new()));
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let url = format!("http://{}", listener.local_addr().unwrap());
        let app = Router::new()
            .route("/v1/analyze", post(record))
            .with_state(events.clone());
        let server = tokio::spawn(async move { axum::serve(listener, app).await.unwrap() });
        let mut state = AppState::new(url.clone());
        state.engine_client = Arc::new(EngineClient::with_key(url, "test-key".into()));

        state.seed_engine_history().await.unwrap();
        let events = events.lock().await;
        assert_eq!(events.len(), 17);
        for chat_id in ["chat_ishaan", "chat_diya"] {
            let history: Vec<_> = events
                .iter()
                .filter(|event| event.channel.conversation_id == chat_id)
                .collect();
            assert!(history
                .windows(2)
                .all(|pair| pair[0].occurred_at < pair[1].occurred_at));
            assert!(history
                .iter()
                .all(|event| event.context.is_contact && !event.draft));
        }
        server.abort();
    }

    #[tokio::test]
    async fn seed_history_reports_engine_failure() {
        let mut state = AppState::new("http://127.0.0.1:1".into());
        state.engine_client = Arc::new(EngineClient::with_key(String::new(), String::new()));
        assert!(matches!(
            state.seed_engine_history().await,
            Err(crate::engine_client::EngineError::Configuration)
        ));
    }
}
