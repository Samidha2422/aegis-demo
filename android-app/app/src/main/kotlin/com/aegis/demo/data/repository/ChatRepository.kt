package com.aegis.demo.data.repository

import com.aegis.demo.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    fun getPersonas(): Flow<List<Persona>>
    suspend fun selectPersona(personaId: String)
    fun getActivePersona(): StateFlow<Persona?>

    fun getChats(): Flow<List<ChatThread>>
    fun getMessages(chatId: String): Flow<List<SafeMessage>>
    suspend fun sendMessage(chatId: String, text: String): Result<SafeMessage>

    suspend fun checkNudge(draft: String): NudgeResult
    suspend fun blockUser(userId: String)
    suspend fun unblockUser(userId: String)
    suspend fun muteUser(userId: String)
    suspend fun submitReport(messageId: String?, userId: String?, reason: String)

    fun getPosts(): Flow<List<Post>>
    suspend fun addComment(postId: String, text: String): Result<Comment>

    fun getCalls(): Flow<List<CallRecord>>
    suspend fun simulateCall(callerId: String)

    fun getAlerts(): Flow<List<AlertItem>>
    suspend fun getBaselineComparison(conversationId: String): BaselineComparison
    suspend fun exportEvidence(format: String): String

    fun playScenario(scenarioId: Int, speed: Int): Flow<ScenarioProgress>
    suspend fun resetDemo()

    fun isReplayMode(): Boolean
    fun setReplayMode(enabled: Boolean)
}
