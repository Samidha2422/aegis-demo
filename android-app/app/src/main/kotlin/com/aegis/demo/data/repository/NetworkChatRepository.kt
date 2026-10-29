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
import kotlinx.serialization.json.*
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
    private val importedCalls = MutableStateFlow<List<CallRecord>>(emptyList())
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _alerts = MutableStateFlow<List<AlertItem>>(emptyList())
    private val _scenarioSteps = MutableSharedFlow<ScenarioProgress>(replay = 1)

    private var webSocket: WebSocket? = null

    private val connectionError = MutableStateFlow<String?>(null)
    private var running = false

    override fun getConnectionError(): Flow<String?> = connectionError.asStateFlow()

    override suspend fun refresh() {
        refreshAll()
    }

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
                                    _calls.update { current -> listOf(call) + current.filterNot { it.id == call.id } }
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

            override fun onOpen(webSocket: WebSocket, response: Response) {
                connectionError.value = null
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
                connectionError.value = null
                }
            } catch (e: Exception) { recordError(e) }

            try {
                val actor = _activePersona.value?.id ?: "meera"
                val chats = apiService.getChats(actor)
                if ((_activePersona.value?.id ?: "meera") == actor) _chats.value = chats
                connectionError.value = null
            } catch (e: Exception) { recordError(e) }

            try {
                _posts.value = apiService.getPosts()
            } catch (e: Exception) { recordError(e) }

            try {
                val serverCalls = apiService.getCalls()
                val imported = importedCalls.value
                val ids = imported.map { it.id }.toSet()
                _calls.update { imported + serverCalls.filterNot { it.id in ids } }
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
            val simulated = if (filename == "demo_proxy_nsfw.png") {
                "true".toRequestBody("text/plain".toMediaTypeOrNull())
            } else null
            val msg = apiService.uploadMedia(chatId, part, kind, sender, simulated)
            appendMessage(msg)
            connectionError.value = null
            Result.success(msg)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    override suspend fun sendVoiceNote(chatId: String, audioBytes: ByteArray, filename: String): Result<SafeMessage> {
        return try {
            require(filename.endsWith(".wav", ignoreCase = true)) { "Choose a WAV audio file." }
            val part = MultipartBody.Part.createFormData("file", filename, audioBytes.toRequestBody("audio/wav".toMediaTypeOrNull()))
            val kind = "voice".toRequestBody("text/plain".toMediaTypeOrNull())
            val sender = (_activePersona.value?.id ?: "meera").toRequestBody("text/plain".toMediaTypeOrNull())
            val simulated = if (filename == "synthetic_audio.wav") {
                "true".toRequestBody("text/plain".toMediaTypeOrNull())
            } else null
            val msg = apiService.uploadMedia(chatId, part, kind, sender, simulated)
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

    override suspend fun simulateCall(
        callerId: String,
        durationSecs: Int,
        status: String,
        transcript: String?
    ): Result<CallRecord> {
        return try {
            val body = mutableMapOf(
                "caller_id" to callerId,
                "duration_secs" to durationSecs.toString(),
                "status" to status
            )
            if (!transcript.isNullOrBlank()) {
                body["transcript"] = transcript
            }
            val record = apiService.simulateCall(body)
            _calls.update { current -> listOf(record) + current.filterNot { it.id == record.id } }
            connectionError.value = null
            Result.success(record)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    override suspend fun importCallLogCsv(csvText: String): Result<List<CallRecord>> {
        return try {
            val records = parseCallCsv(csvText)
            val body = csvText.toRequestBody("text/csv".toMediaTypeOrNull())
            val imported = apiService.importCallLog(body)
            val assessed = imported.rows.mapNotNull { row ->
                val record = records.getOrNull(row.row - 2) ?: return@mapNotNull null
                if (row.outcome !in setOf("imported", "cached") || row.problem != null) return@mapNotNull null
                val assessment = row.assessment as? JsonObject ?: return@mapNotNull null
                val reasons = assessment["reasons"] as? JsonArray
                record.copy(safety = record.safety.copy(
                    severity = assessment["severity"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    tier = assessment["tier"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                    reasons = reasons?.mapNotNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull } ?: emptyList(),
                    recommended_action = assessment["recommended_action"]?.jsonPrimitive?.contentOrNull ?: "log",
                    state = "Delivered",
                    tags = if (record.safety.simulated) listOf("SIMULATED") else emptyList()
                ))
            }
            val ids = assessed.map { it.id }.toSet()
            importedCalls.update { current -> assessed + current.filterNot { it.id in ids } }
            _calls.update { current -> assessed + current.filterNot { it.id in ids } }
            val failed = imported.rows.size - assessed.size
            if (failed > 0 || imported.rows.size != records.size) {
                val message = "Call import incomplete: ${assessed.size} of ${records.size} calls assessed. Some rows may already be saved."
                connectionError.value = message
                return Result.failure(IllegalStateException(message))
            }
            connectionError.value = null
            Result.success(assessed)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    override suspend fun confirmSlangTerm(term: String): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("Confirm slang terms in the moderator dashboard."))
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
            importedCalls.value = emptyList()
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
