package com.aegis.demo.data.repository

import org.junit.Assert.*
import org.junit.Test

class CallCsvTest {
    private val header = "event_id,occurred_at,caller_id,callee_id,duration_s,answered,voicemail,after_refusal,simulated"
    private val row = "b2d80da1-2029-43a4-95b1-147de0994c65,2026-10-28T02:03:00Z,\"caller, one\",meera,14,true,false,false,false"

    @Test fun parsesQuotedCallerAndPreservesSimulationFlag() {
        val call = parseCallCsv("$header\n$row").single()
        assertEquals("caller, one", call.caller_id)
        assertEquals("answered", call.status)
        assertFalse(call.safety.simulated)
        assertEquals("EngineUnavailable", call.safety.state)
    }

    @Test fun rejectsDuplicateIdsAndMalformedBooleans() {
        assertTrue(runCatching { parseCallCsv("$header\n$row\n$row") }.isFailure)
        assertTrue(runCatching { parseCallCsv("$header\n${row.replace(",true,", ",yes,")}") }.isFailure)
    }
}
