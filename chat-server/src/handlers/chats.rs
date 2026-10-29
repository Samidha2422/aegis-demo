use crate::action_policy::ActionPolicy;
use crate::models::{
    ChatParticipant, ChatThread, EngineMedia, LastMessagePreview, SafeMessage, WsEvent,
};
use crate::state::AppState;
use axum::{
    extract::{Path, Query, State},
    http::{header, StatusCode},
    response::IntoResponse,
    Json,
};
use serde::Deserialize;
use std::sync::atomic::Ordering;

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

pub const MAX_AUDIO_BYTES: usize = 25 * 1024 * 1024;
const MAX_IMAGE_BYTES: usize = 10 * 1024 * 1024;
const MAX_STORED_MEDIA_BYTES: usize = 100 * 1024 * 1024;

pub async fn get_media(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<impl IntoResponse, StatusCode> {
    let media = state.media.read().await;
    let (mime, bytes) = media.get(&id).ok_or(StatusCode::NOT_FOUND)?;
    Ok((
        [
            (header::CONTENT_TYPE, *mime),
            (header::CACHE_CONTROL, "no-store"),
            (header::X_CONTENT_TYPE_OPTIONS, "nosniff"),
        ],
        bytes.clone(),
    ))
}

pub async fn upload_media(
    State(state): State<AppState>,
    Path(id): Path<String>,
    multipart: axum::extract::Multipart,
) -> Result<Json<SafeMessage>, StatusCode> {
    upload(&state, Some(id), multipart).await
}

pub(super) async fn upload(
    state: &AppState,
    chat_id: Option<String>,
    mut multipart: axum::extract::Multipart,
) -> Result<Json<SafeMessage>, StatusCode> {
    let generation = state.media_generation.load(Ordering::SeqCst);
    let recording = chat_id.is_none();
    let mut id = chat_id.unwrap_or_else(|| "chat_rohan".into());
    let mut bytes = None;
    let mut demo_proxy = false;
    let mut kind = if recording { "audio" } else { "image" }.to_owned();
    let mut sender = default_sender();
    let mut simulated = false;
    while let Some(field) = multipart.next_field().await.map_err(|e| e.status())? {
        match field.name() {
            Some("file") if bytes.is_none() => {
                demo_proxy = field.file_name() == Some("demo_proxy_nsfw.png");
                let data = field.bytes().await.map_err(|e| e.status())?;
                if data.is_empty() {
                    return Err(StatusCode::BAD_REQUEST);
                }
                if data.len() > MAX_AUDIO_BYTES {
                    return Err(StatusCode::PAYLOAD_TOO_LARGE);
                }
                bytes = Some(data.to_vec());
            }
            Some("kind") => kind = field.text().await.map_err(|e| e.status())?,
            Some("sender_id") => sender = field.text().await.map_err(|e| e.status())?,
            Some("simulated") => {
                simulated = field
                    .text()
                    .await
                    .map_err(|e| e.status())?
                    .parse::<bool>()
                    .map_err(|_| StatusCode::BAD_REQUEST)?;
            }
            Some("chat_id") if recording => id = field.text().await.map_err(|e| e.status())?,
            _ => return Err(StatusCode::BAD_REQUEST),
        }
    }
    let audio = matches!(kind.as_str(), "voice" | "audio");
    if (!audio && kind != "image") || (recording && !audio) {
        return Err(StatusCode::UNSUPPORTED_MEDIA_TYPE);
    }
    let bytes = bytes.ok_or(StatusCode::BAD_REQUEST)?;
    if !audio && bytes.len() > MAX_IMAGE_BYTES {
        return Err(StatusCode::PAYLOAD_TOO_LARGE);
    }
    let mime = if audio {
        audio_mime(&bytes)
    } else {
        image_mime(&bytes)
    }
    .ok_or(StatusCode::UNSUPPORTED_MEDIA_TYPE)?;
    let kind = if audio { "audio" } else { "image" };
    let mut event = state.message_event(&sender, &id, "").await?;
    event.kind = kind.into();
    event.context.simulated = simulated;
    event.message = None;
    event.media = Some(EngineMedia { mime: mime.into() });
    let verdict = if audio {
        state
            .engine_client
            .score_audio(&event, bytes.clone(), mime)
            .await
    } else {
        state
            .engine_client
            .score_image(&event, bytes.clone(), mime)
            .await
    };
    let transcript = if audio {
        verdict
            .as_ref()
            .ok()
            .and_then(|verdict| {
                verdict
                    .modules
                    .get("audio")
                    .and_then(|signal| signal.details.get("transcript"))
                    .and_then(serde_json::Value::as_str)
            })
            .map(str::to_owned)
    } else {
        None
    };
    let mut safety = match verdict {
        Ok(verdict) => {
            ActionPolicy::apply(&verdict, kind, event.context.recipient_blocked_sender, 0)
        }
        Err(error) => {
            let mut safety = ActionPolicy::unscored(&error.to_string());
            if event.context.recipient_blocked_sender {
                safety.state = "Held".into();
            }
            safety
        }
    };
    if simulated {
        safety.simulated = true;
        if !safety.tags.iter().any(|tag| tag == "SIMULATED") {
            safety.tags.push("SIMULATED".into());
        }
    }
    let msg = SafeMessage {
        id: event.event_id.clone(),
        chat_id: id,
        sender_id: event.sender_id,
        recipient_id: event.recipient_id,
        kind: kind.into(),
        text: if audio {
            String::new()
        } else if simulated && demo_proxy {
            "DEMO PROXY IMAGE".into()
        } else {
            "Image".into()
        },
        media_url: Some(format!("/media/{}", event.event_id)),
        transcript,
        timestamp: event.occurred_at,
        safety,
    };
    {
        let mut chats = state.chats.write().await;
        if state.media_generation.load(Ordering::SeqCst) != generation {
            return Err(StatusCode::CONFLICT);
        }
        let mut media = state.media.write().await;
        if media.values().map(|(_, data)| data.len()).sum::<usize>() + bytes.len()
            > MAX_STORED_MEDIA_BYTES
        {
            return Err(StatusCode::INSUFFICIENT_STORAGE);
        }
        media.insert(msg.id.clone(), (mime, bytes));
        chats
            .entry(msg.chat_id.clone())
            .or_default()
            .push(msg.clone());
    }
    let _ = state.tx.send(WsEvent {
        event: "message".into(),
        payload: serde_json::to_value(&msg).unwrap_or_default(),
    });
    Ok(Json(msg))
}

fn audio_mime(bytes: &[u8]) -> Option<&'static str> {
    if bytes.starts_with(b"RIFF") && bytes.get(8..12) == Some(b"WAVE") {
        Some("audio/wav")
    } else if bytes.starts_with(b"ID3")
        || (bytes.len() >= 4
            && bytes[0] == 0xff
            && bytes[1] & 0xe0 == 0xe0
            && bytes[1] & 0x18 != 0x08
            && bytes[1] & 0x06 == 0x02
            && bytes[2] & 0xf0 != 0xf0
            && bytes[2] & 0x0c != 0x0c)
    {
        Some("audio/mpeg")
    } else {
        None
    }
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
    use crate::engine_client::EngineClient;
    use axum::{extract::Multipart, routing::post, Router};
    use std::sync::Arc;

    async fn serve(app: Router) -> (String, tokio::task::JoinHandle<()>) {
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let url = format!("http://{}", listener.local_addr().unwrap());
        let server = tokio::spawn(async move { axum::serve(listener, app).await.unwrap() });
        (url, server)
    }

    fn form(bytes: Vec<u8>, fields: &[(&str, &str)]) -> reqwest::multipart::Form {
        // File deliberately precedes kind: limits cannot depend on multipart field order.
        let mut form = reqwest::multipart::Form::new().part(
            "file",
            reqwest::multipart::Part::bytes(bytes)
                .file_name("untrusted.png")
                .mime_str("image/png")
                .unwrap(),
        );
        for (name, value) in fields {
            form = form.text((*name).to_owned(), (*value).to_owned());
        }
        form
    }

    #[tokio::test]
    async fn audio_uploads_forward_exact_bytes_context_and_masked_metadata() {
        async fn analyze(
            headers: axum::http::HeaderMap,
            mut multipart: Multipart,
        ) -> Json<serde_json::Value> {
            assert_eq!(headers["x-api-key"], "test-key");
            let request = multipart.next_field().await.unwrap().unwrap();
            assert_eq!(request.name(), Some("request"));
            let event: serde_json::Value =
                serde_json::from_str(&request.text().await.unwrap()).unwrap();
            assert_eq!(event["kind"], "audio");
            assert!(event.get("message").is_none());
            assert_eq!(event["media"]["mime"], "audio/wav");
            assert_eq!(event["sender_id"], "rohan");
            assert_eq!(event["recipient_id"], "meera");
            assert_eq!(event["channel"]["conversation_id"], "chat_rohan");
            assert_eq!(event["context"]["recipient_blocked_sender"], true);
            assert_eq!(event["context"]["recipient_muted_sender"], true);
            assert!(
                event["context"].get("simulated").is_none()
                    || event["context"]["simulated"] == true
            );
            let audio = multipart.next_field().await.unwrap().unwrap();
            assert_eq!(audio.name(), Some("audio"));
            assert_eq!(audio.content_type(), Some("audio/wav"));
            assert_eq!(
                &audio.bytes().await.unwrap()[..],
                b"RIFF\x00\x01\x02\x03WAVE\xff\x00payload"
            );
            assert!(multipart.next_field().await.unwrap().is_none());
            Json(serde_json::json!({
                "severity": 75, "tier": "high", "reasons": [{"text": "Threat"}],
                "signals": {"audio": {"score": 0.75, "label": "speech", "status": "built",
                    "backend": "whisper", "confidence": 0.9,
                    "details": {"transcript": "Contact [PHONE]", "duration_s": 2.5}}},
                "recommended_action": "throttle", "secondary_actions": ["show_transcript"],
                "simulated": false, "degraded": [], "model_versions": {"audio": "test"}
            }))
        }
        let (engine, engine_task) =
            serve(Router::new().route("/v1/analyze/audio", post(analyze))).await;
        let mut state = AppState::new(engine.clone());
        state.engine_client = Arc::new(EngineClient::with_key(engine, "test-key".into()));
        state
            .blocked_users
            .write()
            .await
            .insert(("meera".into(), "rohan".into()));
        state
            .muted_users
            .write()
            .await
            .insert(("meera".into(), "rohan".into()));
        let app = Router::new()
            .route("/chats/:id/media", post(upload_media))
            .route("/recordings", post(super::super::calls::upload_recording))
            .with_state(state.clone());
        let (url, task) = serve(app).await;
        for (path, simulated) in [("/chats/chat_rohan/media", false), ("/recordings", true)] {
            let mut fields = vec![("kind", "voice"), ("sender_id", "rohan")];
            if simulated {
                fields.push(("simulated", "true"));
            }
            let response = reqwest::Client::new()
                .post(format!("{url}{path}"))
                .multipart(form(
                    b"RIFF\x00\x01\x02\x03WAVE\xff\x00payload".to_vec(),
                    &fields,
                ))
                .send()
                .await
                .unwrap();
            assert_eq!(response.status(), StatusCode::OK);
            let message: SafeMessage = response.json().await.unwrap();
            assert_eq!(message.kind, "audio");
            assert_eq!(message.transcript.as_deref(), Some("Contact [PHONE]"));
            assert!(message.media_url.as_deref().unwrap().starts_with("/media/"));
            assert!(message.text.is_empty());
            assert_eq!(message.safety.state, "Held");
            assert_eq!(message.safety.simulated, simulated);
            assert_eq!(
                message.safety.tags.iter().any(|tag| tag == "SIMULATED"),
                simulated
            );
            assert_eq!(message.safety.modules["audio"].details["duration_s"], 2.5);
            assert_eq!(message.safety.secondary_actions, ["show_transcript"]);
        }
        assert_eq!(state.chats.read().await["chat_rohan"].len(), 2);
        task.abort();
        engine_task.abort();
    }

    #[tokio::test]
    async fn recordings_default_sender_and_chat_and_disabled_audio_stays_unknown() {
        async fn disabled(mut multipart: Multipart) -> Json<serde_json::Value> {
            let event: serde_json::Value = serde_json::from_str(
                &multipart
                    .next_field()
                    .await
                    .unwrap()
                    .unwrap()
                    .text()
                    .await
                    .unwrap(),
            )
            .unwrap();
            assert_eq!(event["sender_id"], "meera");
            assert_eq!(event["kind"], "audio");
            assert_eq!(event["media"]["mime"], "audio/mpeg");
            assert_eq!(
                event["channel"]["conversation_id"],
                format!("chat_{}", event["recipient_id"].as_str().unwrap())
            );
            Json(serde_json::json!({
                "severity": 0, "tier": "low", "reasons": [],
                "signals": {"audio": {"score": 0, "label": "unavailable", "status": "roadmap",
                    "backend": "disabled", "confidence": 0, "details": {"transcript": null, "duration_s": 1}}},
                "recommended_action": "log", "simulated": false,
                "degraded": ["audio.asr_unavailable"], "model_versions": {}
            }))
        }
        let (engine, engine_task) =
            serve(Router::new().route("/v1/analyze/audio", post(disabled))).await;
        let mut state = AppState::new(engine.clone());
        state.engine_client = Arc::new(EngineClient::with_key(engine, "test-key".into()));
        let (url, task) = serve(
            Router::new()
                .route("/recordings", post(super::super::calls::upload_recording))
                .with_state(state),
        )
        .await;
        for chat in [None, Some("chat_ishaan")] {
            let fields = chat
                .map(|id| vec![("chat_id", id), ("simulated", "false")])
                .unwrap_or_default();
            let response = reqwest::Client::new()
                .post(format!("{url}/recordings"))
                .multipart(form(b"ID3\0data".to_vec(), &fields))
                .send()
                .await
                .unwrap();
            assert_eq!(response.status(), StatusCode::OK);
            let message: SafeMessage = response.json().await.unwrap();
            assert_eq!(message.chat_id, chat.unwrap_or("chat_rohan"));
            assert_eq!(message.sender_id, "meera");
            assert!(message.transcript.is_none());
            assert_eq!(message.safety.tier, "unknown");
            assert_eq!(message.safety.state, "EngineUnavailable");
            assert!(message.safety.tags.iter().any(|tag| tag == "UNSCORED"));
            assert!(!message.safety.simulated);
            assert_eq!(message.safety.modules["audio"].backend, "disabled");
        }
        task.abort();
        engine_task.abort();
    }

    #[tokio::test]
    async fn invalid_source_simulation_flag_is_rejected_before_scoring() {
        let state = AppState::new("http://127.0.0.1:1".into());
        let (url, task) = serve(
            Router::new()
                .route("/recordings", post(super::super::calls::upload_recording))
                .with_state(state),
        )
        .await;
        let response = reqwest::Client::new()
            .post(format!("{url}/recordings"))
            .multipart(form(b"ID3\0test".to_vec(), &[("simulated", "yes")]))
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::BAD_REQUEST);
        task.abort();
    }

    #[tokio::test]
    async fn unavailable_audio_is_unscored_and_media_is_cleared_on_reset() {
        let mut state = AppState::new("http://127.0.0.1:1".into());
        state.engine_client = Arc::new(EngineClient::with_key(
            "http://127.0.0.1:1".into(),
            "test-key".into(),
        ));
        let (url, task) = serve(
            Router::new()
                .route("/recordings", post(super::super::calls::upload_recording))
                .route("/media/:id", axum::routing::get(get_media))
                .with_state(state.clone()),
        )
        .await;
        let response = reqwest::Client::new()
            .post(format!("{url}/recordings"))
            .multipart(form(b"ID3\x00test".to_vec(), &[]))
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::OK);
        let message: SafeMessage = response.json().await.unwrap();
        assert_eq!(message.safety.state, "EngineUnavailable");
        assert_eq!(message.safety.tier, "unknown");
        let media_url = format!("{url}{}", message.media_url.unwrap());
        let media = reqwest::get(&media_url).await.unwrap();
        assert_eq!(media.headers()["content-type"], "audio/mpeg");
        assert_eq!(media.headers()["cache-control"], "no-store");
        assert_eq!(&media.bytes().await.unwrap()[..], b"ID3\x00test");
        state.reset().await;
        assert_eq!(
            reqwest::get(&media_url).await.unwrap().status(),
            StatusCode::NOT_FOUND
        );
        task.abort();
    }

    #[tokio::test]
    async fn full_media_store_rejects_upload_without_storing_message() {
        let state = AppState::new("http://127.0.0.1:1".into());
        state.media.write().await.insert(
            "existing".into(),
            ("image/png", vec![0; MAX_STORED_MEDIA_BYTES]),
        );
        let (url, task) = serve(
            Router::new()
                .route("/chats/:id/media", post(upload_media))
                .with_state(state.clone()),
        )
        .await;
        let response = reqwest::Client::new()
            .post(format!("{url}/chats/chat_rohan/media"))
            .multipart(form(
                b"\x89PNG\r\n\x1a\nimage".to_vec(),
                &[("kind", "image")],
            ))
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::INSUFFICIENT_STORAGE);
        assert!(!state.chats.read().await.contains_key("chat_rohan"));
        task.abort();
    }

    #[tokio::test]
    async fn kind_limits_apply_after_file_even_when_kind_is_last() {
        let state = AppState::new("http://127.0.0.1:1".into());
        let (url, task) = serve(
            Router::new()
                .route("/chats/:id/media", post(upload_media))
                .layer(axum::extract::DefaultBodyLimit::max(
                    MAX_AUDIO_BYTES + 65536,
                ))
                .with_state(state),
        )
        .await;
        let client = reqwest::Client::new();
        let response = client
            .post(format!("{url}/chats/chat_rohan/media"))
            .multipart(form(vec![0; MAX_IMAGE_BYTES + 1], &[("kind", "image")]))
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::PAYLOAD_TOO_LARGE);
        // Audio above the image limit reaches MIME validation rather than the image size rejection.
        let response = client
            .post(format!("{url}/chats/chat_rohan/media"))
            .multipart(form(vec![0; MAX_IMAGE_BYTES + 1], &[("kind", "audio")]))
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::UNSUPPORTED_MEDIA_TYPE);
        let response = client
            .post(format!("{url}/chats/chat_rohan/media"))
            .multipart(form(vec![0; MAX_AUDIO_BYTES + 1], &[("kind", "audio")]))
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::PAYLOAD_TOO_LARGE);
        task.abort();
    }

    #[test]
    fn audio_mime_requires_wav_or_mp3_bytes() {
        assert_eq!(audio_mime(b"RIFF\0\0\0\0WAVEdata"), Some("audio/wav"));
        assert_eq!(audio_mime(b"ID3data"), Some("audio/mpeg"));
        assert_eq!(audio_mime(b"\xff\xfb\x90\x00"), Some("audio/mpeg"));
        assert_eq!(audio_mime(b"RIFF\0\0\0\0WEBPdata"), None);
        assert_eq!(audio_mime(b"\xff\xd8\xff"), None);
        assert_eq!(audio_mime(b"\xff\xff\xff\xff"), None);
        assert_eq!(audio_mime(b""), None);
    }
    #[test]
    fn mime_comes_from_bytes_instead_of_untrusted_filename() {
        assert_eq!(image_mime(b"\x89PNG\r\n\x1a\nrest"), Some("image/png"));
        assert_eq!(image_mime(b"not an image"), None);
    }
}
