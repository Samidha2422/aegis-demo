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

class NetworkChatRepository(
    private val apiService: ChatApiService,
    private val okHttpClient: OkHttpClient,
    private val wsUrl: String = "ws://localhost:8080/ws"
) : ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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

    init {
        initWebSocket()
        refreshAll()
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
                    initWebSocket()
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
            } catch (e: Exception) {}

            try {
                _chats.value = apiService.getChats()
            } catch (e: Exception) {}

            try {
                _posts.value = apiService.getPosts()
            } catch (e: Exception) {}

            try {
                _calls.value = apiService.getCalls()
            } catch (e: Exception) {}
        }
    }

    private fun appendMessage(msg: SafeMessage) {
        val current = _messages.value.toMutableMap()
        val list = (current[msg.chat_id] ?: emptyList()).toMutableList()
        val existingIndex = list.indexOfFirst { it.id == msg.id }
        if (existingIndex >= 0) {
            list[existingIndex] = msg
        } else {
            list.add(msg)
        }
        current[msg.chat_id] = list
        _messages.value = current
    }

    private fun updateMessageSafety(msg: SafeMessage) {
        appendMessage(msg)
    }

    override fun getPersonas(): Flow<List<Persona>> = _personas.asStateFlow()

    override suspend fun selectPersona(personaId: String) {
        try {
            apiService.login(mapOf("persona_id" to personaId))
            val p = _personas.value.find { it.id == personaId }
            _activePersona.value = p
            refreshAll()
        } catch (e: Exception) {}
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
            } catch (e: Exception) {}
        }
        return _messages.map { it[chatId] ?: emptyList() }
    }

    override suspend fun sendMessage(chatId: String, text: String): Result<SafeMessage> {
        return try {
            val msg = apiService.sendMessage(chatId, mapOf("text" to text, "kind" to "text"))
            appendMessage(msg)
            Result.success(msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkNudge(draft: String): NudgeResult {
        return try {
            apiService.checkNudge(mapOf("draft" to draft))
        } catch (e: Exception) {
            NudgeResult(0f, "low", emptyList(), "log", null)
        }
    }

    override suspend fun blockUser(userId: String) {
        try {
            apiService.blockUser(userId)
            val current = _chats.value.toMutableList()
            val idx = current.indexOfFirst { it.participant.id == userId }
            if (idx >= 0) {
                val item = current[idx]
                current[idx] = item.copy(participant = item.participant.copy(is_blocked = true))
                _chats.value = current
            }
        } catch (e: Exception) {}
    }

    override suspend fun unblockUser(userId: String) {
        try {
            apiService.unblockUser(userId)
            val current = _chats.value.toMutableList()
            val idx = current.indexOfFirst { it.participant.id == userId }
            if (idx >= 0) {
                val item = current[idx]
                current[idx] = item.copy(participant = item.participant.copy(is_blocked = false))
                _chats.value = current
            }
        } catch (e: Exception) {}
    }

    override suspend fun muteUser(userId: String) {
        try {
            apiService.muteUser(userId)
            val current = _chats.value.toMutableList()
            val idx = current.indexOfFirst { it.participant.id == userId }
            if (idx >= 0) {
                val item = current[idx]
                current[idx] = item.copy(participant = item.participant.copy(is_muted = true))
                _chats.value = current
            }
        } catch (e: Exception) {}
    }

    override suspend fun submitReport(messageId: String?, userId: String?, reason: String) {
        try {
            val map = mutableMapOf("reason" to reason)
            messageId?.let { map["message_id"] = it }
            userId?.let { map["user_id"] = it }
            apiService.submitReport(map)
        } catch (e: Exception) {}
    }

    override fun getPosts(): Flow<List<Post>> = _posts.asStateFlow()

    override suspend fun addComment(postId: String, text: String): Result<Comment> {
        return try {
            val c = apiService.addComment(postId, mapOf("content" to text))
            refreshAll()
            Result.success(c)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCalls(): Flow<List<CallRecord>> = _calls.asStateFlow()

    override suspend fun simulateCall(callerId: String) {
        try {
            apiService.simulateCall(mapOf("caller_id" to callerId))
        } catch (e: Exception) {}
    }

    override fun getAlerts(): Flow<List<AlertItem>> = _alerts.asStateFlow()

    override suspend fun getBaselineComparison(conversationId: String): BaselineComparison {
        return try {
            apiService.getBaseline(conversationId)
        } catch (e: Exception) {
            BaselineComparison(
                conversation_id = conversationId,
                total_messages = 80,
                keyword_filter_flags = 0,
                keyword_flagged_reasons = emptyList(),
                engine_max_tier = "high",
                engine_max_severity = 84f,
                engine_reasons = listOf("Behavioral anomaly detected"),
                behavioral_insight = "Fallback baseline comparison."
            )
        }
    }

    override suspend fun exportEvidence(format: String): String {
        return try {
            apiService.exportEvidence(format)
        } catch (e: Exception) {
            "{}"
        }
    }

    override fun playScenario(scenarioId: Int, speed: Int): Flow<ScenarioProgress> {
        scope.launch {
            try {
                apiService.playScenario(scenarioId, speed)
            } catch (e: Exception) {}
        }
        return _scenarioSteps.asSharedFlow()
    }

    override suspend fun resetDemo() {
        try {
            apiService.resetDemo()
            refreshAll()
        } catch (e: Exception) {}
    }

    override fun isReplayMode(): Boolean = isReplay

    override fun setReplayMode(enabled: Boolean) {
        isReplay = enabled
    }
}
