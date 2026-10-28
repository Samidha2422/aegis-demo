use crate::{
    action_policy::ActionPolicy,
    models::{Comment, EngineEventEnvelope, Post},
    state::AppState,
};
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use chrono::Utc;
use serde::Deserialize;

#[derive(Deserialize)]
pub struct AddCommentRequest {
    #[serde(default = "super::chats::default_sender")]
    pub author_id: String,
    pub content: String,
}

pub async fn list_posts(State(state): State<AppState>) -> Json<Vec<Post>> {
    let mut posts = state.posts.write().await;
    if posts.is_empty() {
        posts.push(Post {
            id: "post_meera_01".into(),
            author_id: "meera".into(),
            content: "Excited to share our new design prototype! Let me know what you think."
                .into(),
            timestamp: Utc::now(),
            comments_count: 0,
            is_protected: false,
            comments: Vec::new(),
        });
    }
    Json(posts.clone())
}

pub async fn add_comment(
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(payload): Json<AddCommentRequest>,
) -> Result<Json<Comment>, StatusCode> {
    if payload.content.trim().is_empty()
        || payload.content.len() > 32768
        || payload.author_id.trim().is_empty()
        || payload.author_id.len() > 256
    {
        return Err(StatusCode::UNPROCESSABLE_ENTITY);
    }
    let recipient = state
        .posts
        .read()
        .await
        .iter()
        .find(|p| p.id == id)
        .map(|p| p.author_id.clone())
        .ok_or(StatusCode::NOT_FOUND)?;
    let mut event =
        EngineEventEnvelope::message(&payload.author_id, &recipient, &id, &payload.content);
    event.channel.kind = "comment".into();
    event.channel.post_id = Some(id.clone());
    let refusal_pair = (recipient.clone(), payload.author_id.clone());
    event.context.recipient_blocked_sender =
        state.blocked_users.read().await.contains(&refusal_pair);
    event.context.recipient_muted_sender = state.muted_users.read().await.contains(&refusal_pair);
    let safety = match state.engine_client.score_event(&event).await {
        Ok(v) => ActionPolicy::apply(&v, "text", event.context.recipient_blocked_sender, 0),
        Err(e) => ActionPolicy::unscored(&e.to_string()),
    };
    let comment = Comment {
        id: event.event_id,
        post_id: id.clone(),
        author_id: payload.author_id,
        content: payload.content,
        timestamp: event.occurred_at,
        safety,
    };
    let mut posts = state.posts.write().await;
    let post = posts
        .iter_mut()
        .find(|p| p.id == id)
        .ok_or(StatusCode::NOT_FOUND)?;
    post.is_protected |= matches!(comment.safety.tier.as_str(), "high" | "urgent");
    post.comments.push(comment.clone());
    post.comments_count = post.comments.len();
    Ok(Json(comment))
}
