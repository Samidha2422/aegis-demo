package com.aegis.demo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Persona(
    val id: String,
    val name: String,
    val avatar_url: String,
    val role: String,
    val is_contact: Boolean,
    val bio: String
)

@Serializable
data class ChatThread(
    val id: String,
    val participant: ChatParticipant,
    val last_message: LastMessagePreview? = null,
    val unread_count: Int = 0,
    val held_count: Int = 0,
    val max_tier: String = "low"
)

@Serializable
data class ChatParticipant(
    val id: String,
    val name: String,
    val avatar_url: String,
    val is_contact: Boolean = true,
    val is_blocked: Boolean = false,
    val is_muted: Boolean = false
)

@Serializable
data class LastMessagePreview(
    val id: String,
    val sender_id: String,
    val text: String,
    val timestamp: String,
    val safety_tier: String
)

@Serializable
data class SafeMessage(
    val id: String,
    val chat_id: String,
    val sender_id: String,
    val recipient_id: String,
    val kind: String = "text", // "text", "image", "voice"
    val text: String = "",
    val media_url: String? = null,
    val transcript: String? = null,
    val timestamp: String,
    val safety: SafetyObject = SafetyObject()
)

@Serializable
data class SafetyObject(
    val severity: Float = 0f,
    val tier: String = "low", // "low", "review", "high", "urgent"
    val reasons: List<String> = emptyList(),
    val signals: Map<String, Float> = emptyMap(),
    val recommended_action: String = "log",
    val state: String = "Delivered", // "Delivered", "Scanning", "Warned", "Blurred", "Hidden", "Held", "Escalated", "TranscriptOnly", "EngineUnavailable"
    val simulated: Boolean = false,
    val tags: List<String> = emptyList()
)

@Serializable
data class Post(
    val id: String,
    val author_id: String,
    val content: String,
    val timestamp: String,
    val comments_count: Int = 0,
    val is_protected: Boolean = false,
    val comments: List<Comment> = emptyList()
)

@Serializable
data class Comment(
    val id: String,
    val post_id: String,
    val author_id: String,
    val content: String,
    val timestamp: String,
    val safety: SafetyObject = SafetyObject()
)

@Serializable
data class CallRecord(
    val id: String,
    val caller_id: String,
    val callee_id: String,
    val timestamp: String,
    val duration_secs: Int = 0,
    val status: String = "missed", // "answered", "missed", "rejected"
    val safety: SafetyObject = SafetyObject()
)

@Serializable
data class AlertItem(
    val id: String,
    val tier: String,
    val title: String,
    val description: String,
    val target_user_id: String,
    val timestamp: String,
    val reasons: List<String> = emptyList()
)

@Serializable
data class BaselineComparison(
    val conversation_id: String,
    val total_messages: Int,
    val keyword_filter_flags: Int,
    val keyword_flagged_reasons: List<String> = emptyList(),
    val engine_max_tier: String,
    val engine_max_severity: Float,
    val engine_reasons: List<String> = emptyList(),
    val behavioral_insight: String
)

@Serializable
data class ScenarioProgress(
    val scenario_id: Int,
    val step_index: Int,
    val total_steps: Int,
    val actor: String,
    val kind: String,
    val description: String,
    val expect_tier: String,
    val occurred_at: String
)

@Serializable
data class NudgeResult(
    val severity: Float,
    val tier: String,
    val reasons: List<String>,
    val recommended_action: String,
    val nudge_text: String? = null
)
