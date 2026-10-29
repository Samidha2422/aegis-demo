package com.aegis.demo.data.repository

import com.aegis.demo.data.model.CallRecord
import com.aegis.demo.data.model.SafetyObject
import java.time.OffsetDateTime
import java.util.UUID

internal fun parseCallCsv(text: String): List<CallRecord> {
    val lines = text.removePrefix("\uFEFF").trim().lines()
    require(lines.isNotEmpty() && lines.first().isNotBlank()) { "Empty CSV" }
    fun fields(line: String): List<String> {
        val result = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closed = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                quoted && c == '"' -> { quoted = false; closed = true }
                quoted -> field.append(c)
                c == ',' -> { result.add(field.toString()); field.clear(); closed = false }
                c == '"' && field.isEmpty() && !closed -> quoted = true
                c == '"' || closed -> throw IllegalArgumentException("Invalid CSV quoting")
                else -> field.append(c)
            }
            i++
        }
        require(!quoted) { "Invalid CSV quoting" }
        result.add(field.toString())
        return result
    }
    val header = fields(lines.first().trimEnd('\r'))
    val required = listOf("event_id", "occurred_at", "caller_id", "callee_id", "duration_s", "answered", "voicemail", "after_refusal", "simulated")
    require(header.size == required.size && header.toSet() == required.toSet()) { "CSV must use the nine engine call-log columns" }
    val indices = required.map { header.indexOf(it) }
    require(lines.size <= 10001 && text.toByteArray(Charsets.UTF_8).size <= 2 * 1024 * 1024) { "CSV exceeds import limits" }
    val records = lines.drop(1).mapIndexed { index, line ->
        val cols = fields(line.trimEnd('\r'))
        require(cols.size == header.size) { "Invalid CSV row ${index + 2}" }
        fun get(i: Int) = cols[indices[i]]
        fun flag(i: Int): Boolean = when (get(i)) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Invalid boolean on row ${index + 2}")
        }
        val id = UUID.fromString(get(0)).toString()
        require(id != "00000000-0000-0000-0000-000000000000") { "Invalid event ID" }
        OffsetDateTime.parse(get(1))
        require(get(2).isNotBlank() && get(3).isNotBlank() && get(2) != get(3)) { "Invalid caller or callee" }
        val duration = get(4).toIntOrNull()
        require(duration != null && duration in 0..86400) { "Invalid duration" }
        val answered = flag(5)
        flag(6); flag(7)
        val simulated = flag(8)
        CallRecord(id, get(2), get(3), get(1), duration, if (answered) "answered" else "missed",
            SafetyObject(tier = "unknown", state = "EngineUnavailable", simulated = simulated, tags = listOf("UNSCORED")))
    }
    require(records.map { it.id }.toSet().size == records.size) { "Duplicate event ID" }
    require(records.isNotEmpty()) { "CSV has no calls" }
    return records
}
