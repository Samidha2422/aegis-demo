use crate::models::{Comment, Post, SafetyObject};
use crate::state::AppState;
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use chrono::Utc;
use serde::Deserialize;
use uuid::Uuid;

#[derive(Deserialize)]
pub struct AddCommentRequest {
    pub author_id: String,
    pub content: String,
}

pub async fn list_posts(State(state): State<AppState>) -> Json<Vec<Post>> {
    let mut posts = state.posts.read().await.clone();
    if posts.is_empty() {
        // Initialize default post for Meera if empty
        let default_post = Post {
            id: "post_meera_01".to_string(),
            author_id: "meera".to_string(),
            content: "Excited to share our new design prototype! Let me know what you think in the comments below.".to_string(),
            timestamp: Utc::now() - chrono::Duration::hours(2),
            comments_count: 0,
            is_protected: false,
            comments: Vec::new(),
        };
        posts.push(default_post);
    }
    Json(posts)
}

pub async fn add_comment(
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(payload): Json<AddCommentRequest>,
) -> Result<Json<Comment>, StatusCode> {
    let mut posts = state.posts.write().await;
    let post = posts
        .iter_mut()
        .find(|p| p.id == id)
        .ok_or(StatusCode::NOT_FOUND)?;

    let is_bot = payload.author_id.starts_with("bot");
    let (severity, tier, reasons) = if is_bot {
        (
            82.0,
            "high".to_string(),
            vec!["Coordinated volume spike: pile-on comment from non-contact account".to_string()],
        )
    } else {
        (0.0, "low".to_string(), Vec::new())
    };

    let safety = SafetyObject {
        severity,
        tier,
        reasons,
        signals: std::collections::HashMap::new(),
        recommended_action: if is_bot {
            "throttle".to_string()
        } else {
            "log".to_string()
        },
        state: if is_bot {
            "Warned".to_string()
        } else {
            "Delivered".to_string()
        },
        simulated: is_bot,
        tags: if is_bot {
            vec!["SIMULATED BOT".to_string()]
        } else {
            Vec::new()
        },
    };

    let comment = Comment {
        id: format!("comment_{}", Uuid::new_v4().to_string().replace('-', "")),
        post_id: id,
        author_id: payload.author_id,
        content: payload.content,
        timestamp: Utc::now(),
        safety,
    };

    post.comments.push(comment.clone());
    post.comments_count = post.comments.len();

    // Check pile-on threshold
    let bot_count = post
        .comments
        .iter()
        .filter(|c| c.author_id.starts_with("bot"))
        .count();
    if bot_count >= 5 {
        post.is_protected = true;
    }

    Ok(Json(comment))
}
