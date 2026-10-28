package com.aegis.demo.data.repository

import com.aegis.demo.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.UUID

class FixtureRepository : ChatRepository {
    private var isReplay: Boolean = true

    private val defaultPersonas = listOf(
        Persona("meera", "Meera Patel", "assets/avatars/meera.png", "victim", true, "Designer & active social user"),
        Persona("rohan", "Rohan Sharma", "assets/avatars/rohan.png", "stalker", false, "Persistent acquaintance"),
        Persona("ishaan", "Ishaan Verma", "assets/avatars/ishaan.png", "partner", true, "Meera's partner"),
        Persona("diya", "Diya Sen", "assets/avatars/diya.png", "partner", true, "Friend & colleague"),
        Persona("stranger_77", "Unknown User (77)", "assets/avatars/unknown.png", "cyberflasher", false, ""),
        Persona("mod_asha", "Asha (Moderator)", "assets/avatars/mod.png", "moderator", true, "Safety operations team")
    )

    private val _activePersona = MutableStateFlow<Persona?>(defaultPersonas[0])
    private val _chats = MutableStateFlow<List<ChatThread>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<SafeMessage>>>(emptyMap())
    private val _calls = MutableStateFlow<List<CallRecord>>(emptyList())
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _alerts = MutableStateFlow<List<AlertItem>>(emptyList())

    init {
        seedInitialFixtures()
    }

    private fun seedInitialFixtures() {
        val initialChats = listOf(
            ChatThread(
                id = "chat_rohan",
                participant = ChatParticipant("rohan", "Rohan Sharma", "assets/avatars/rohan.png", is_contact = false),
                last_message = LastMessagePreview("msg_r80", "rohan", "Are you free this weekend?", "10:15 AM", "high"),
                unread_count = 50,
                held_count = 30,
                max_tier = "high"
            ),
            ChatThread(
                id = "chat_ishaan",
                participant = ChatParticipant("ishaan", "Ishaan Verma", "assets/avatars/ishaan.png", is_contact = true),
                last_message = LastMessagePreview("msg_i10", "ishaan", "Perfect, table is already booked. See you tonight!", "09:30 AM", "low"),
                unread_count = 0,
                held_count = 0,
                max_tier = "low"
            ),
            ChatThread(
                id = "chat_diya",
                participant = ChatParticipant("diya", "Diya Sen", "assets/avatars/diya.png", is_contact = true),
                last_message = LastMessagePreview("msg_d07", "diya", "Saturday 11 AM at our spot. Can't wait!", "10:05 AM", "low"),
                unread_count = 0,
                held_count = 0,
                max_tier = "low"
            ),
            ChatThread(
                id = "chat_stranger",
                participant = ChatParticipant("stranger_77", "Unknown User (77)", "assets/avatars/unknown.png", is_contact = false),
                last_message = LastMessagePreview("msg_s01", "stranger_77", "[Photo]", "08:10 AM", "high"),
                unread_count = 1,
                held_count = 0,
                max_tier = "high"
            )
        )
        _chats.value = initialChats

        // Seed messages for Rohan (Scenario 1 & 4)
        val rohanMessages = mutableListOf<SafeMessage>()
        for (i in 1..80) {
            val tier = if (i < 30) "low" else if (i < 60) "review" else "high"
            val state = if (i < 30) "Delivered" else if (i < 60) "Warned" else "Held"
            val reasons = if (i >= 60) {
                listOf("80 messages in 3 hours, 0 replies", "Contact attempt after block")
            } else if (i >= 30) {
                listOf("High frequency one-sided messaging (30+ messages)")
            } else emptyList()

            rohanMessages.add(
                SafeMessage(
                    id = "msg_r$i",
                    chat_id = "chat_rohan",
                    sender_id = "rohan",
                    recipient_id = "meera",
                    text = "Hey Meera, message #$i! Hope you're having a wonderful day.",
                    timestamp = "10:${String.format("%02d", i % 60)} AM",
                    safety = SafetyObject(
                        severity = if (i >= 60) 84f else if (i >= 30) 45f else 10f,
                        tier = tier,
                        reasons = reasons,
                        recommended_action = if (i >= 60) "throttle" else if (i >= 30) "warn_sender" else "log",
                        state = state,
                        simulated = false,
                        tags = emptyList()
                    )
                )
            )
        }

        // Scenario 4: Screenshot meme in Rohan's chat
        rohanMessages.add(
            SafeMessage(
                id = "msg_r81_meme",
                chat_id = "chat_rohan",
                sender_id = "rohan",
                recipient_id = "meera",
                kind = "image",
                text = "Check this out",
                media_url = "assets/screenshot_meme.png",
                timestamp = "10:16 AM",
                safety = SafetyObject(
                    severity = 79f,
                    tier = "high",
                    reasons = listOf("Extracted text contains harassment: 'You will never escape our comments'"),
                    signals = mapOf("ocr_toxicity" to 0.88f),
                    recommended_action = "blur_media",
                    state = "Blurred",
                    simulated = true,
                    tags = listOf("DEMO PROXY", "OCR FLAGGED")
                )
            )
        )

        // Stranger 77 messages (Scenario 3: Cyberflashing)
        val strangerMessages = listOf(
            SafeMessage(
                id = "msg_s01",
                chat_id = "chat_stranger",
                sender_id = "stranger_77",
                recipient_id = "meera",
                kind = "image",
                text = "",
                media_url = "assets/proxy_nsfw.png",
                timestamp = "08:10 AM",
                safety = SafetyObject(
                    severity = 88f,
                    tier = "high",
                    reasons = listOf("Unsolicited explicit media from non-contact", "DEMO PROXY / SIMULATED SIGNAL"),
                    signals = mapOf("nsfw_score" to 0.94f),
                    recommended_action = "blur_media",
                    state = "Blurred",
                    simulated = true,
                    tags = listOf("DEMO PROXY", "SIMULATED SIGNAL")
                )
            )
        )

        val ishaanMessages = listOf(
            SafeMessage(
                id = "msg_i01",
                chat_id = "chat_ishaan",
                sender_id = "meera",
                recipient_id = "ishaan",
                text = "Morning! Are you heading to the office soon?",
                timestamp = "08:30 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i02",
                chat_id = "chat_ishaan",
                sender_id = "ishaan",
                recipient_id = "meera",
                text = "Leaving in 10 mins. Want me to grab coffee?",
                timestamp = "08:35 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i03",
                chat_id = "chat_ishaan",
                sender_id = "meera",
                recipient_id = "ishaan",
                text = "Oat latte from the corner cafe please!",
                timestamp = "08:38 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i04",
                chat_id = "chat_ishaan",
                sender_id = "ishaan",
                recipient_id = "meera",
                text = "You got it! See you at 10.",
                timestamp = "08:40 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i05",
                chat_id = "chat_ishaan",
                sender_id = "meera",
                recipient_id = "ishaan",
                text = "Thanks so much, you're the best!",
                timestamp = "09:00 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i06",
                chat_id = "chat_ishaan",
                sender_id = "ishaan",
                recipient_id = "meera",
                text = "Haha of course! How's the user research doc?",
                timestamp = "09:10 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i07",
                chat_id = "chat_ishaan",
                sender_id = "meera",
                recipient_id = "ishaan",
                text = "Almost done, findings look really positive.",
                timestamp = "09:15 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i08",
                chat_id = "chat_ishaan",
                sender_id = "ishaan",
                recipient_id = "meera",
                text = "Nice! Still good for dinner tonight?",
                timestamp = "09:20 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i09",
                chat_id = "chat_ishaan",
                sender_id = "meera",
                recipient_id = "ishaan",
                text = "Yes! 8pm at the Italian place?",
                timestamp = "09:25 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_i10",
                chat_id = "chat_ishaan",
                sender_id = "ishaan",
                recipient_id = "meera",
                text = "Perfect, table is already booked. See you tonight!",
                timestamp = "09:30 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            )
        )

        val diyaMessages = listOf(
            SafeMessage(
                id = "msg_d01",
                chat_id = "chat_diya",
                sender_id = "diya",
                recipient_id = "meera",
                text = "Meera!! Did you see the research acceptance email?!",
                timestamp = "Yesterday",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_d02",
                chat_id = "chat_diya",
                sender_id = "meera",
                recipient_id = "diya",
                text = "OMG YES! I couldn't believe it when I saw it!",
                timestamp = "Yesterday",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_d03",
                chat_id = "chat_diya",
                sender_id = "diya",
                recipient_id = "meera",
                text = "So well deserved!! The design system research was incredible.",
                timestamp = "Yesterday",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_d04",
                chat_id = "chat_diya",
                sender_id = "meera",
                recipient_id = "diya",
                text = "Couldn't have done it without your data help, honestly.",
                timestamp = "Yesterday",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_d05",
                chat_id = "chat_diya",
                sender_id = "diya",
                recipient_id = "meera",
                text = "Team effort! We have to celebrate this weekend.",
                timestamp = "Yesterday",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_d06",
                chat_id = "chat_diya",
                sender_id = "meera",
                recipient_id = "diya",
                text = "Definitely! Saturday brunch?",
                timestamp = "10:00 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            ),
            SafeMessage(
                id = "msg_d07",
                chat_id = "chat_diya",
                sender_id = "diya",
                recipient_id = "meera",
                text = "Saturday 11 AM at our spot. Can't wait!",
                timestamp = "10:05 AM",
                safety = SafetyObject(0f, "low", emptyList(), state = "Delivered")
            )
        )

        _messages.value = mapOf(
            "chat_rohan" to rohanMessages,
            "chat_stranger" to strangerMessages,
            "chat_ishaan" to ishaanMessages,
            "chat_diya" to diyaMessages
        )

        // Seed Calls (Scenario 6)
        val callsList = mutableListOf<CallRecord>()
        for (i in 1..8) {
            callsList.add(
                CallRecord(
                    id = "call_stalker_$i",
                    caller_id = "unknown_number",
                    callee_id = "meera",
                    timestamp = "02:${String.format("%02d", 10 + i * 5)} AM",
                    duration_secs = 0,
                    status = "missed",
                    safety = SafetyObject(
                        severity = 82f,
                        tier = "high",
                        reasons = listOf("40 calls overnight, 92% unanswered", "Contact after block"),
                        recommended_action = "throttle",
                        state = "Escalated",
                        simulated = true,
                        tags = listOf("SIMULATED CALL LOG")
                    )
                )
            )
        }
        _calls.value = callsList

        // Seed Alerts
        _alerts.value = listOf(
            AlertItem(
                id = "alert_01",
                tier = "high",
                title = "Persistent Stalking Detected",
                description = "80 messages sent in 3 hours with 0 replies and contact attempts after blocking.",
                target_user_id = "rohan",
                timestamp = "10:15 AM",
                reasons = listOf("80 messages in 3 h, 0 replies", "Contact attempt after block")
            ),
            AlertItem(
                id = "alert_02",
                tier = "high",
                title = "Repeated Overnight Calls",
                description = "40 calls from Unknown Number during quiet hours.",
                target_user_id = "unknown_number",
                timestamp = "03:45 AM",
                reasons = listOf("Overnight frequency spike", "92% unanswered calls")
            )
        )
    }

    override fun getPersonas(): Flow<List<Persona>> = flowOf(defaultPersonas)

    override suspend fun selectPersona(personaId: String) {
        val found = defaultPersonas.find { it.id == personaId }
        _activePersona.value = found ?: defaultPersonas[0]
    }

    override fun getActivePersona(): StateFlow<Persona?> = _activePersona.asStateFlow()

    override fun getChats(): Flow<List<ChatThread>> = _chats.asStateFlow()

    override fun getMessages(chatId: String): Flow<List<SafeMessage>> =
        _messages.map { it[chatId] ?: emptyList() }

    override suspend fun sendMessage(chatId: String, text: String): Result<SafeMessage> {
        val msg = SafeMessage(
            id = "msg_${UUID.randomUUID()}",
            chat_id = chatId,
            sender_id = "meera",
            recipient_id = chatId.removePrefix("chat_"),
            text = text,
            timestamp = "Just now",
            safety = SafetyObject(state = "Delivered", tier = "low")
        )
        val current = _messages.value.toMutableMap()
        val list = (current[chatId] ?: emptyList()).toMutableList()
        list.add(msg)
        current[chatId] = list
        _messages.value = current
        return Result.success(msg)
    }

    override suspend fun sendImage(chatId: String, imageBytes: ByteArray, filename: String): Result<SafeMessage> {
        val isStranger = chatId.contains("stranger") || chatId.contains("unknown")
        val safety = if (isStranger) {
            SafetyObject(
                severity = 82f,
                tier = "high",
                reasons = listOf(
                    "Unsolicited media from non-contact",
                    "NSFW inference connecting (rule degradation active)"
                ),
                signals = mapOf("heuristic_risk" to 0.82f),
                recommended_action = "blur_media",
                state = "Blurred",
                simulated = true,
                tags = listOf("DEMO PROXY IMAGE", "SIMULATED SIGNAL", "DEGRADATION: RULE FALLBACK")
            )
        } else {
            SafetyObject(
                severity = 12f,
                tier = "low",
                reasons = listOf(
                    "Media shared within active contact thread",
                    "NSFW inference connecting (clean heuristic)"
                ),
                signals = mapOf("heuristic_risk" to 0.12f),
                recommended_action = "log",
                state = "Delivered",
                simulated = true,
                tags = listOf("DEMO PROXY IMAGE", "SIMULATED")
            )
        }

        val msg = SafeMessage(
            id = "msg_${UUID.randomUUID()}",
            chat_id = chatId,
            sender_id = _activePersona.value?.id ?: "meera",
            recipient_id = chatId.removePrefix("chat_"),
            kind = "image",
            text = filename,
            media_url = "assets/proxy_nsfw.png",
            timestamp = "Just now",
            safety = safety
        )

        val current = _messages.value.toMutableMap()
        val list = (current[chatId] ?: emptyList()).toMutableList()
        list.add(msg)
        current[chatId] = list
        _messages.value = current
        return Result.success(msg)
    }

    override suspend fun sendVoiceNote(chatId: String, audioBytes: ByteArray, filename: String): Result<SafeMessage> {
        val msg = SafeMessage(
            id = "msg_${UUID.randomUUID()}",
            chat_id = chatId,
            sender_id = _activePersona.value?.id ?: "meera",
            recipient_id = chatId.removePrefix("chat_"),
            kind = "audio",
            text = "Voice Note (${audioBytes.size} bytes)",
            media_url = "assets/synthetic_audio.wav",
            transcript = "Voice note recorded and analyzed by Aegis Engine.",
            timestamp = "Just now",
            safety = SafetyObject(
                severity = 15f,
                tier = "low",
                reasons = listOf("Voice note acoustic features within benign boundaries"),
                signals = mapOf("audio_toxicity" to 0.12f),
                recommended_action = "log",
                state = "Delivered",
                simulated = true,
                tags = listOf("SYNTHETIC AUDIO", "VOICE NOTE")
            )
        )
        val current = _messages.value.toMutableMap()
        val list = (current[chatId] ?: emptyList()).toMutableList()
        list.add(msg)
        current[chatId] = list
        _messages.value = current
        return Result.success(msg)
    }

    override suspend fun checkNudge(draft: String, chatId: String): NudgeResult {
        val lower = draft.lowercase()
        val isFriend = chatId in listOf("chat_ishaan", "chat_diya")
        val isTeasingBanter = (lower.contains("idiot") || lower.contains("crazy") || lower.contains("annoying")) &&
                (lower.contains("lol") || lower.contains("haha") || lower.contains("why are you"))

        if (isFriend && isTeasingBanter) {
            // Reciprocal contact caps contextual risk (Healthy Reciprocity)
            return NudgeResult(
                severity = 28f,
                tier = "low",
                reasons = listOf("Reciprocal contact caps contextual risk (Healthy Reciprocity)"),
                recommended_action = "allow",
                nudge_text = null,
                simulated = true
            )
        }

        val matchingSlang = confirmedSlangTerms.firstOrNull { lower.contains(it) }
        if (matchingSlang != null) {
            return NudgeResult(
                severity = 86f,
                tier = "high",
                reasons = listOf("Draft contains confirmed community slang term: '$matchingSlang'"),
                recommended_action = "warn_sender",
                nudge_text = "This message contains confirmed community harassment slang ('$matchingSlang'). Please revise."
            )
        }

        val isAbusive = lower.contains("idiot") || lower.contains("pathetic") || lower.contains("hate you")
        return if (isAbusive) {
            NudgeResult(
                severity = 75f,
                tier = "review",
                reasons = listOf("Draft contains potentially hostile or harassing language"),
                recommended_action = "warn_sender",
                nudge_text = "This message may hurt someone. Would you like to edit it before sending?"
            )
        } else {
            NudgeResult(severity = 0f, tier = "low", reasons = emptyList(), recommended_action = "log")
        }
    }

    override suspend fun blockUser(userId: String) {
        val currentChats = _chats.value.toMutableList()
        val idx = currentChats.indexOfFirst { it.participant.id == userId }
        if (idx >= 0) {
            val item = currentChats[idx]
            currentChats[idx] = item.copy(participant = item.participant.copy(is_blocked = true))
            _chats.value = currentChats
        }
    }

    override suspend fun unblockUser(userId: String) {
        val currentChats = _chats.value.toMutableList()
        val idx = currentChats.indexOfFirst { it.participant.id == userId }
        if (idx >= 0) {
            val item = currentChats[idx]
            currentChats[idx] = item.copy(participant = item.participant.copy(is_blocked = false))
            _chats.value = currentChats
        }
    }

    override suspend fun muteUser(userId: String) {
        val currentChats = _chats.value.toMutableList()
        val idx = currentChats.indexOfFirst { it.participant.id == userId }
        if (idx >= 0) {
            val item = currentChats[idx]
            currentChats[idx] = item.copy(participant = item.participant.copy(is_muted = true))
            _chats.value = currentChats
        }
    }

    override suspend fun submitReport(messageId: String?, userId: String?, reason: String) {}

    override fun getPosts(): Flow<List<Post>> = _posts.asStateFlow()

    override suspend fun addComment(postId: String, text: String): Result<Comment> {
        val comment = Comment(
            id = "comment_${UUID.randomUUID()}",
            post_id = postId,
            author_id = "meera",
            content = text,
            timestamp = "Just now"
        )
        return Result.success(comment)
    }

    override fun getCalls(): Flow<List<CallRecord>> = _calls.asStateFlow()

    override suspend fun simulateCall(
        callerId: String,
        durationSecs: Int,
        status: String,
        transcript: String?
    ): Result<CallRecord> {
        val hasAggressive = transcript?.contains("Pick up", ignoreCase = true) == true ||
                transcript?.contains("threat", ignoreCase = true) == true ||
                transcript?.contains("ignore", ignoreCase = true) == true
        val score = if (hasAggressive) 88f else 18f
        val newCall = CallRecord(
            id = "call_sim_${UUID.randomUUID()}",
            caller_id = callerId,
            callee_id = "meera",
            timestamp = "Just now",
            duration_secs = durationSecs,
            status = status,
            safety = SafetyObject(
                severity = score,
                tier = if (score >= 70f) "high" else "low",
                reasons = if (hasAggressive) {
                    listOf("Hostile voice transcript pattern", "Quiet-hour repeated attempt spike")
                } else {
                    listOf("Benign call pattern")
                },
                recommended_action = if (hasAggressive) "throttle" else "allow",
                state = if (hasAggressive) "Escalated" else "Delivered",
                simulated = true,
                tags = listOf("SIMULATED CALL")
            ),
            transcript = transcript
        )
        _calls.value = listOf(newCall) + _calls.value
        return Result.success(newCall)
    }

    private val confirmedSlangTerms = mutableSetOf<String>()

    override suspend fun confirmSlangTerm(term: String): Result<Boolean> {
        confirmedSlangTerms.add(term.lowercase().trim())
        return Result.success(true)
    }

    override suspend fun importCallLogCsv(csvText: String): Result<List<CallRecord>> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return Result.failure(IllegalArgumentException("Empty CSV"))
        val rows = if (lines[0].startsWith("call_id")) lines.drop(1) else lines
        val parsed = rows.mapIndexed { idx, line ->
            val cols = line.split(",").map { it.trim() }
            val caller = cols.getOrNull(1) ?: "unknown_stalker"
            val callee = cols.getOrNull(2) ?: "meera"
            val rawTs = cols.getOrNull(3) ?: "02:00 AM"
            val ts = if (rawTs.contains("T")) {
                val timePart = rawTs.substringAfter("T").substringBefore("Z")
                val hourMin = timePart.split(":")
                val h = hourMin.getOrNull(0)?.toIntOrNull() ?: 2
                val m = hourMin.getOrNull(1) ?: "00"
                String.format("%02d:%s AM", h, m)
            } else rawTs
            val dur = cols.getOrNull(4)?.toIntOrNull() ?: 0
            val stat = cols.getOrNull(5) ?: "missed"
            CallRecord(
                id = "call_imported_$idx",
                caller_id = caller,
                callee_id = callee,
                timestamp = ts,
                duration_secs = dur,
                status = stat,
                safety = SafetyObject(
                    severity = 94f,
                    tier = "high",
                    reasons = listOf(
                        "CSV Call Log: 40 calls overnight, 92% unanswered",
                        "Quiet-hour repeated attempt spike (02:00-04:00 AM)"
                    ),
                    recommended_action = "throttle",
                    state = "Escalated",
                    simulated = true,
                    tags = listOf("IMPORTED CSV", "SIMULATED CALL LOG")
                ),
                transcript = if (stat == "answered" && dur > 0) "Hello? Why do you keep calling me in the middle of the night?" else null
            )
        }
        _calls.value = parsed + _calls.value
        return Result.success(parsed)
    }

    override fun getAlerts(): Flow<List<AlertItem>> = _alerts.asStateFlow()

    override suspend fun getBaselineComparison(conversationId: String): BaselineComparison {
        return BaselineComparison(
            conversation_id = conversationId,
            total_messages = 80,
            keyword_filter_flags = 0,
            keyword_flagged_reasons = emptyList(),
            engine_max_tier = "high",
            engine_max_severity = 84f,
            engine_reasons = listOf(
                "80 messages in 3 hours, 0 replies",
                "Contact attempts persisting after user block",
                "Behavioral escalation pattern"
            ),
            behavioral_insight = "Keyword filter passed all 80 messages because each message was politely worded. The Aegis Risk Engine detected high volume, 0 replies, and contact attempts persisting after a block—proving behavioral AI detects what keywords miss."
        )
    }

    override suspend fun exportEvidence(format: String): String {
        return """
            {
              "case_id": "AEGIS-CASE-2026-HACK",
              "victim_id": "meera",
              "integrity_hash": "sha256-e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
              "flagged_events_count": 85,
              "summary": "Forensic timeline of persistent stalking, polite harassment, and post-block evasion."
            }
        """.trimIndent()
    }

    override fun playScenario(scenarioId: Int, speed: Int): Flow<ScenarioProgress> = flow {
        val total = 5
        for (i in 1..total) {
            delay(1000L / (speed.coerceAtLeast(1)))
            emit(
                ScenarioProgress(
                    scenario_id = scenarioId,
                    step_index = i,
                    total_steps = total,
                    actor = "rohan",
                    kind = "message",
                    description = "Step $i of scenario $scenarioId",
                    expect_tier = if (i >= 3) "high" else "low",
                    occurred_at = "10:${String.format("%02d", i * 10)} AM"
                )
            )
        }
    }

    override suspend fun resetDemo() {
        seedInitialFixtures()
    }

    override fun isReplayMode(): Boolean = isReplay

    override fun setReplayMode(enabled: Boolean) {
        isReplay = enabled
    }
}
