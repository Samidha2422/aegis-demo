package com.aegis.demo.data.repository

import com.aegis.demo.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    fun getConnectionError(): Flow<String?> = kotlinx.coroutines.flow.flowOf(null)
    suspend fun getEngineStatus(): EngineStatus = EngineStatus(EngineHealth("replay", "fixtures", true, emptyList()), emptyList())
    suspend fun refresh() {}
    fun getPersonas(): Flow<List<Persona>>
    suspend fun selectPersona(personaId: String)
    fun getActivePersona(): StateFlow<Persona?>

    fun getChats(): Flow<List<ChatThread>>
    fun getMessages(chatId: String): Flow<List<SafeMessage>>
    suspend fun sendMessage(chatId: String, text: String): Result<SafeMessage>
    suspend fun sendImage(chatId: String, imageBytes: ByteArray, filename: String = "image.jpg"): Result<SafeMessage>
    suspend fun sendVoiceNote(chatId: String, audioBytes: ByteArray, filename: String = "voice.wav"): Result<SafeMessage>

    suspend fun checkNudge(draft: String, chatId: String = "chat_rohan"): NudgeResult
    suspend fun blockUser(userId: String)
    suspend fun unblockUser(userId: String)
    suspend fun muteUser(userId: String)
    suspend fun submitReport(messageId: String?, userId: String?, reason: String)

    fun getPosts(): Flow<List<Post>>
    suspend fun addComment(postId: String, text: String): Result<Comment>

    fun getCalls(): Flow<List<CallRecord>>
    suspend fun simulateCall(
        callerId: String,
        durationSecs: Int = 0,
        status: String = "missed",
        transcript: String? = null
    ): Result<CallRecord>
    suspend fun importCallLogCsv(csvText: String): Result<List<CallRecord>>

    fun getAlerts(): Flow<List<AlertItem>>
    suspend fun getBaselineComparison(conversationId: String): BaselineComparison
    suspend fun confirmSlangTerm(term: String): Result<Boolean>
    suspend fun exportEvidence(format: String): String

    fun playScenario(scenarioId: Int, speed: Int): Flow<ScenarioProgress>
    suspend fun resetDemo()

    fun isReplayMode(): Boolean
    fun setReplayMode(enabled: Boolean)
}
