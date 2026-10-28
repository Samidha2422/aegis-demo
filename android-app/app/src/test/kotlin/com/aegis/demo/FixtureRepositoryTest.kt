package com.aegis.demo

import com.aegis.demo.data.repository.FixtureRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FixtureRepositoryTest {

    private lateinit var repository: FixtureRepository

    @Before
    fun setUp() {
        repository = FixtureRepository()
    }

    @Test
    fun testInitialPersonasContainsMeeraAndRohan() = runTest {
        val personas = repository.getPersonas().first()
        assertTrue("Should contain meera", personas.any { it.id == "meera" })
        assertTrue("Should contain rohan", personas.any { it.id == "rohan" })
        val active = repository.getActivePersona().value
        assertNotNull(active)
        assertEquals("meera", active?.id)
    }

    @Test
    fun testChatsListHasHeldMessagesForStalkingScenario() = runTest {
        val chats = repository.getChats().first()
        val rohanChat = chats.find { it.id == "chat_rohan" }
        assertNotNull("Rohan chat thread must exist", rohanChat)
        assertTrue("Rohan chat should have held messages", (rohanChat?.held_count ?: 0) > 0)
        assertEquals("high", rohanChat?.max_tier)
    }

    @Test
    fun testPreSendNudgeFlagsToxicLanguage() = runTest {
        val safeDraft = repository.checkNudge("Hey, are you free tomorrow for lunch?")
        assertEquals("low", safeDraft.tier)
        assertNull(safeDraft.nudge_text)

        val toxicDraft = repository.checkNudge("You are an idiot and I hate you!")
        assertEquals("review", toxicDraft.tier)
        assertNotNull(toxicDraft.nudge_text)
        assertTrue("Should contain warning text", toxicDraft.nudge_text!!.contains("hurt"))
    }

    @Test
    fun testBlockUserUpdatesChatParticipant() = runTest {
        repository.blockUser("rohan")
        val chats = repository.getChats().first()
        val rohanChat = chats.find { it.participant.id == "rohan" }
        assertTrue("Participant should be blocked", rohanChat?.participant?.is_blocked == true)

        repository.unblockUser("rohan")
        val updatedChats = repository.getChats().first()
        val unblockedChat = updatedChats.find { it.participant.id == "rohan" }
        assertFalse("Participant should be unblocked", unblockedChat?.participant?.is_blocked == true)
    }

    @Test
    fun testBaselineComparisonReflectsKeywordVsEngine() = runTest {
        val comparison = repository.getBaselineComparison("chat_rohan")
        assertEquals(80, comparison.total_messages)
        assertEquals("Keyword filter flags should be 0 because all messages were polite", 0, comparison.keyword_filter_flags)
        assertEquals("high", comparison.engine_max_tier)
        assertTrue(comparison.engine_max_severity >= 80f)
        assertTrue(comparison.behavioral_insight.contains("polite"))
    }
}
