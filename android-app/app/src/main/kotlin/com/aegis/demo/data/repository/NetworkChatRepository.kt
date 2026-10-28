package com.aegis.demo.data.repository

import com.aegis.demo.data.api.ChatApiService
import com.aegis.demo.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class NetworkChatRepository(
    private val apiService: ChatApiService,
    private val okHttpClient: OkHttpClient,
    private val wsUrl: String = "ws://10.0.2.2:8081/ws"
) : ChatRepository {

    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var isReplay: Boolean = false
    private val _personas = MutableStateFlow<List<Persona>>(emptyList())
    private val _activePersona = MutableStateFlow<Persona?>(null)
    private val _chats = MutableStateFlow<List<ChatThread>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<SafeMessage>>>(emptyMap())
    private val _calls = MutableStateFlow<List<CallRecord>>(emptyList())
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _alerts = MutableStateFlow<List<AlertItem>>(emptyList())
    private val _scenarioSteps = MutableSharedFlow<ScenarioProgress>(replay = 1)

    private var webSocket: WebSocket? = null

    private val connectionError = MutableStateFlow<String?>(null)
    private var running = false

    override fun getConnectionError(): Flow<String?> = connectionError.asStateFlow()

    override suspend fun getEngineStatus(): EngineStatus = try {
        apiService.getEngineStatus().also { connectionError.value = null }
    } catch (e: Exception) {
        recordError(e)
        throw e
    }

    fun start() {
        if (running) return
        running = true
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        initWebSocket()
        refreshAll()
    }

    fun close() {
        running = false
        webSocket?.cancel()
        webSocket = null
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    private fun recordError(error: Exception) {
        if (error is kotlinx.coroutines.CancellationException) throw error
        connectionError.value = if (error is retrofit2.HttpException) {
            "Chat server request failed (HTTP ${error.code()}). Check engine status in Settings."
        } else {
            "Chat server unavailable. Check the server URL in Settings."
        }
    }
    private fun initWebSocket() {
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val tree = json.parseToJsonElement(text).jsonObject
                        val eventType = tree["event"]?.toString()?.replace("\"", "")
                        val payload = tree["payload"]

                        when (eventType) {
                            "message" -> {
                                payload?.let {
                                    val msg = json.decodeFromJsonElement<SafeMessage>(it)
                                    appendMessage(msg)
                                }
                            }
                            "safety_update" -> {
                                payload?.let {
                                    val msg = json.decodeFromJsonElement<SafeMessage>(it)
                                    updateMessageSafety(msg)
                                }
                            }
                            "call_event" -> {
                                payload?.let {
                                    val call = json.decodeFromJsonElement<CallRecord>(it)
                                    _calls.value = listOf(call) + _calls.value
                                }
                            }
                            "alert" -> {
                                payload?.let {
                                    val alert = json.decodeFromJsonElement<AlertItem>(it)
                                    _alerts.value = listOf(alert) + _alerts.value
                                }
                            }
                            "scenario_step" -> {
                                payload?.let {
                                    val step = json.decodeFromJsonElement<ScenarioProgress>(it)
                                    _scenarioSteps.emit(step)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore or log payload parsing errors in stream
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    delay(3000)
                    if (running) initWebSocket()
                }
            }
        })
    }

    fun refreshAll() {
        scope.launch {
            try {
                _personas.value = apiService.getPersonas()
                if (_activePersona.value == null && _personas.value.isNotEmpty()) {
                    _activePersona.value = _personas.value[0]
                }
            } catch (e: Exception) { recordError(e) }

            try {
                val actor = _activePersona.value?.id ?: "meera"
                val chats = apiService.getChats(actor)
                if ((_activePersona.value?.id ?: "meera") == actor) _chats.value = chats
            } catch (e: Exception) { recordError(e) }

            try {
                _posts.value = apiService.getPosts()
            } catch (e: Exception) { recordError(e) }

            try {
                _calls.value = apiService.getCalls()
            } catch (e: Exception) { recordError(e) }
        }
    }

    private fun requireSuccess(response: retrofit2.Response<Unit>) {
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
    }
    private fun appendMessage(msg: SafeMessage) {
        _messages.update { current ->
            val list = current[msg.chat_id].orEmpty()
            current + (msg.chat_id to if (list.any { it.id == msg.id }) list.map { if (it.id == msg.id) msg else it } else list + msg)
        }
        refreshAll()
    }

    private fun updateMessageSafety(msg: SafeMessage) {
        appendMessage(msg)
    }

    override fun getPersonas(): Flow<List<Persona>> = _personas.asStateFlow()

    override suspend fun selectPersona(personaId: String) {
        try {
            val login = apiService.login(mapOf("persona_id" to personaId))
            _activePersona.value = login.persona
            _chats.value = emptyList()
            refreshAll()
        } catch (e: Exception) { recordError(e) }
    }

    override fun getActivePersona(): StateFlow<Persona?> = _activePersona.asStateFlow()

    override fun getChats(): Flow<List<ChatThread>> = _chats.asStateFlow()

    override fun getMessages(chatId: String): Flow<List<SafeMessage>> {
        scope.launch {
            try {
                val msgs = apiService.getMessages(chatId)
                val current = _messages.value.toMutableMap()
                current[chatId] = msgs
                _messages.value = current
            } catch (e: Exception) { recordError(e) }
        }
        return _messages.map { it[chatId] ?: emptyList() }
    }

    override suspend fun sendMessage(chatId: String, text: String): Result<SafeMessage> {
        return try {
            val msg = apiService.sendMessage(chatId, mapOf("text" to text, "sender_id" to (_activePersona.value?.id ?: "meera")))
            appendMessage(msg)
            connectionError.value = null
            Result.success(msg)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    override suspend fun sendImage(chatId: String, imageBytes: ByteArray, filename: String): Result<SafeMessage> {
        return try {
            val part = MultipartBody.Part.createFormData("file", filename, imageBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
            val kind = "image".toRequestBody("text/plain".toMediaTypeOrNull())
            val sender = (_activePersona.value?.id ?: "meera").toRequestBody("text/plain".toMediaTypeOrNull())
            val msg = apiService.uploadMedia(chatId, part, kind, sender)
            appendMessage(msg)
            connectionError.value = null
            Result.success(msg)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    override suspend fun checkNudge(draft: String, chatId: String): NudgeResult {
        return try {
            apiService.checkNudge(mapOf("draft" to draft, "chat_id" to chatId, "sender_id" to (_activePersona.value?.id ?: "meera")))
        } catch (e: Exception) {
            recordError(e)
            NudgeResult(0f, "unknown", emptyList(), "log", "Draft safety check unavailable", available = false)
        }
    }
    override suspend fun blockUser(userId: String) {
        try {
            requireSuccess(apiService.blockUser(userId, mapOf("actor_id" to (_activePersona.value?.id ?: "meera"))))
            val current = _chats.value.toMutableList()
            val idx = current.indexOfFirst { it.participant.id == userId }
            if (idx >= 0) {
                val item = current[idx]
                current[idx] = item.copy(participant = item.participant.copy(is_blocked = true))
                _chats.value = current
            }
        } catch (e: Exception) { recordError(e) }
    }

    override suspend fun unblockUser(userId: String) {
        try {
            requireSuccess(apiService.unblockUser(userId, mapOf("actor_id" to (_activePersona.value?.id ?: "meera"))))
            val current = _chats.value.toMutableList()
            val idx = current.indexOfFirst { it.participant.id == userId }
            if (idx >= 0) {
                val item = current[idx]
                current[idx] = item.copy(participant = item.participant.copy(is_blocked = false))
                _chats.value = current
            }
        } catch (e: Exception) { recordError(e) }
    }

    override suspend fun muteUser(userId: String) {
        try {
            requireSuccess(apiService.muteUser(userId, mapOf("actor_id" to (_activePersona.value?.id ?: "meera"))))
            val current = _chats.value.toMutableList()
            val idx = current.indexOfFirst { it.participant.id == userId }
            if (idx >= 0) {
                val item = current[idx]
                current[idx] = item.copy(participant = item.participant.copy(is_muted = true))
                _chats.value = current
            }
        } catch (e: Exception) { recordError(e) }
    }

    override suspend fun submitReport(messageId: String?, userId: String?, reason: String) {
        try {
            val map = mutableMapOf("reason" to reason)
            messageId?.let { map["message_id"] = it }
            userId?.let { map["user_id"] = it }
            requireSuccess(apiService.submitReport(map))
        } catch (e: Exception) { recordError(e) }
    }

    override fun getPosts(): Flow<List<Post>> = _posts.asStateFlow()

    override suspend fun addComment(postId: String, text: String): Result<Comment> {
        return try {
            val c = apiService.addComment(postId, mapOf("content" to text, "author_id" to (_activePersona.value?.id ?: "meera")))
            refreshAll()
            Result.success(c)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    override fun getCalls(): Flow<List<CallRecord>> = _calls.asStateFlow()

    override suspend fun simulateCall(callerId: String) {
        try {
            apiService.simulateCall(mapOf("caller_id" to callerId))
        } catch (e: Exception) { recordError(e) }
    }

    override fun getAlerts(): Flow<List<AlertItem>> = _alerts.asStateFlow()

    override suspend fun getBaselineComparison(conversationId: String): BaselineComparison {
        return try {
            apiService.getBaseline(conversationId)
        } catch (e: Exception) {
            recordError(e)
            BaselineComparison(conversationId, 0, 0, emptyList(), "unknown", 0f, emptyList(), "Comparison unavailable")
        }
    }

    override suspend fun exportEvidence(format: String): String {
        return try {
            apiService.exportEvidence(format).use { it.string() }
        } catch (e: Exception) {
            recordError(e)
            "Evidence export unavailable"
        }
    }
    override fun playScenario(scenarioId: Int, speed: Int): Flow<ScenarioProgress> {
        scope.launch {
            try {
                requireSuccess(apiService.playScenario(scenarioId, speed))
            } catch (e: Exception) { recordError(e) }
        }
        return _scenarioSteps.asSharedFlow()
    }

    override suspend fun resetDemo() {
        try {
            requireSuccess(apiService.resetDemo())
            _messages.value = emptyMap()
            _alerts.value = emptyList()
            refreshAll()
        } catch (e: Exception) { recordError(e) }
    }

    override fun isReplayMode(): Boolean = isReplay

    override fun setReplayMode(enabled: Boolean) {
        isReplay = enabled
    }
}
