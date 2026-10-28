package com.aegis.demo

import com.aegis.demo.data.api.ChatApiService
import com.aegis.demo.data.model.*
import com.aegis.demo.data.repository.NetworkChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
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
