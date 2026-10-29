package com.aegis.demo

import com.aegis.demo.data.api.ChatApiService
import com.aegis.demo.data.model.*
import com.aegis.demo.data.repository.NetworkChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy

class NetworkChatRepositoryTest {
    private fun api(handler: (String, Array<out Any?>) -> Any?): ChatApiService =
        Proxy.newProxyInstance(ChatApiService::class.java.classLoader, arrayOf(ChatApiService::class.java)) { _, method, args ->
            handler(method.name, args ?: emptyArray())
        } as ChatApiService

    @Test
    fun failedImageUploadDoesNotInventMessageOrVerdict() = runTest {
        val repository = NetworkChatRepository(api { _, _ -> throw IOException("offline") }, OkHttpClient())
        assertTrue(repository.sendImage("chat_rohan", byteArrayOf(1), "photo.png").isFailure)
        assertTrue(repository.getMessages("chat_rohan").first().isEmpty())
        assertNotNull(repository.getConnectionError().first())
        repository.close()
    }

    @Test
    fun failedDraftIsUnavailableRatherThanClean() = runTest {
        val repository = NetworkChatRepository(api { _, _ -> throw IOException("offline") }, OkHttpClient())
        val nudge = repository.checkNudge("hello", "chat_rohan")
        assertFalse(nudge.available)
        assertEquals("unknown", nudge.tier)
        repository.close()
    }

    @Test
    fun draftIncludesConversationAndDoesNotSendMessage() = runTest {
        val calls = mutableListOf<String>()
        val repository = NetworkChatRepository(api { name, args ->
            calls.add(name)
            assertEquals("checkNudge", name)
            val body = args[0] as Map<*, *>
            assertEquals("chat_ishaan", body["chat_id"])
            assertEquals("meera", body["sender_id"])
            NudgeResult(0f, "low", emptyList(), "log")
        }, OkHttpClient())
        repository.checkNudge("hello", "chat_ishaan")
        assertEquals(listOf("checkNudge"), calls)
        repository.close()
    }

    @Test
    fun refusalsSendSelectedActorForEveryAction() = runTest {
        val persona = Persona("rohan", "Rohan", "", "stalker", false, "")
        val refusals = mutableListOf<String>()
        val repository = NetworkChatRepository(api { name, args ->
            when (name) {
                "login" -> LoginResult("demo-session", persona)
                "blockUser", "unblockUser", "muteUser" -> {
                    assertEquals("meera", args[0])
                    assertEquals("rohan", (args[1] as Map<*, *>)["actor_id"])
                    refusals.add(name)
                    retrofit2.Response.success<Unit>(Unit)
                }
                else -> throw IOException("Background refresh unavailable")
            }
        }, OkHttpClient())
        repository.selectPersona("rohan")
        repository.blockUser("meera")
        repository.unblockUser("meera")
        repository.muteUser("meera")
        assertEquals(listOf("blockUser", "unblockUser", "muteUser"), refusals)
        repository.close()
    }

    private fun RequestBody.text(): String = Buffer().also { writeTo(it) }.readUtf8()

    @Test
    fun voiceUploadMarksOnlyExplicitSyntheticSample() = runTest {
        val uploads = mutableListOf<String?>()
        val filenames = listOf("synthetic_audio.wav", "voice.wav", "my_synthetic_audio.wav")
        val repository = NetworkChatRepository(api { name, args ->
            if (name != "uploadMedia") throw IOException("Background refresh unavailable")
            assertEquals("chat_rohan", args[0])
            val file = args[1] as MultipartBody.Part
            assertTrue(file.headers?.get("Content-Disposition").orEmpty().contains("filename=\"${filenames[uploads.size]}\""))
            assertArrayEquals(byteArrayOf(1), Buffer().also { file.body.writeTo(it) }.readByteArray())
            assertEquals("audio/wav", file.body.contentType().toString())
            assertEquals("voice", (args[2] as RequestBody).text())
            assertEquals("meera", (args[3] as RequestBody).text())
            uploads.add((args[4] as RequestBody?)?.text())
            SafeMessage("voice", "chat_rohan", "meera", "rohan", kind = "voice", timestamp = "now")
        }, OkHttpClient())
        try {
            for (filename in filenames) {
                assertTrue(repository.sendVoiceNote("chat_rohan", byteArrayOf(1), filename).isSuccess)
            }
            assertEquals(listOf("true", null, null), uploads)
        } finally {
            repository.close()
        }
    }

    @Test
    fun unsupportedAudioDoesNotUpload() = runTest {
        var uploaded = false
        val repository = NetworkChatRepository(api { _, _ ->
            uploaded = true
            throw IOException("Must not upload unsupported audio")
        }, OkHttpClient())
        try {
            val result = repository.sendVoiceNote("chat_rohan", byteArrayOf(1), "voice.mp3")
            assertTrue(result.isFailure)
            assertEquals("Choose a WAV audio file.", result.exceptionOrNull()?.message)
            assertFalse(uploaded)
        } finally {
            repository.close()
        }
    }

    @Test
    fun imageUploadMarksOnlyExplicitProxy() = runTest {
        val uploads = mutableListOf<String?>()
        val repository = NetworkChatRepository(api { name, args ->
            if (name != "uploadMedia") throw IOException("Background refresh unavailable")
            assertEquals("image", (args[2] as RequestBody).text())
            uploads.add((args[4] as RequestBody?)?.text())
            SafeMessage("image", "chat_rohan", "meera", "rohan", kind = "image", timestamp = "now")
        }, OkHttpClient())
        try {
            for (filename in listOf("demo_proxy_nsfw.png", "photo.png", "my_demo_proxy_nsfw.png")) {
                assertTrue(repository.sendImage("chat_rohan", byteArrayOf(1), filename).isSuccess)
            }
            assertEquals(listOf("true", null, null), uploads)
        } finally {
            repository.close()
        }
    }

    @Test
    fun safetyDetailsAndSecondaryActionsSurviveDeserialization() {
        val safety = Json.decodeFromString<SafetyObject>("""
            {"secondary_actions":["alert_moderator"],"modules":{
                "audio":{"score":0.8,"label":"speech","status":"built","backend":"whisper","confidence":0.9,
                    "details":{"duration_s":2.5,"transcribed":true,"language":"en","nested":{"tokens":3},"segments":[1,2]}}
            }}
        """.trimIndent())
        val details = safety.modules.getValue("audio").details
        assertEquals(JsonPrimitive(2.5), details["duration_s"])
        assertEquals(JsonPrimitive(true), details["transcribed"])
        assertEquals(JsonPrimitive("en"), details["language"])
        assertEquals(Json.parseToJsonElement("""{"tokens":3}"""), details["nested"])
        assertEquals(Json.parseToJsonElement("[1,2]"), details["segments"])
        assertEquals(listOf("alert_moderator"), safety.secondary_actions)
    }

    @Test
    fun olderSafetyPayloadsKeepEmptyDefaults() {
        val safety = Json.decodeFromString<SafetyObject>("""
            {"modules":{"audio":{"score":0.2,"label":"speech","status":"built","backend":"whisper","confidence":0.8}}}
        """.trimIndent())
        assertTrue(safety.secondary_actions.isEmpty())
        assertTrue(safety.modules.getValue("audio").details.isEmpty())
    }

    @Test
    fun safetyMetadataPreservesBackendAndSimulation() {
        val safety = Json { ignoreUnknownKeys = true }.decodeFromString<SafetyObject>("""
            {"severity":42,"tier":"review","simulated":true,"degraded":["text.onnx_missing"],
            "modules":{"text":{"score":0.4,"label":"toxicity","status":"built","backend":"heuristic","confidence":0.5}}}
        """.trimIndent())
        assertTrue(safety.simulated)
        assertEquals("heuristic", safety.modules["text"]?.backend)
        assertEquals(listOf("text.onnx_missing"), safety.degraded)
    }
}
