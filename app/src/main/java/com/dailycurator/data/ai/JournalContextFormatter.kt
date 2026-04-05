package com.dailycurator.data.ai

import com.dailycurator.data.model.JournalEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val stampFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

/** Builds a compact text block of recent journal entries for LLM context (chat / insights). */
object JournalContextFormatter {

    fun format(entries: List<JournalEntry>, maxBodyCharsPerEntry: Int = 450): String {
        if (entries.isEmpty()) return "None"
        return buildString {
            entries.forEach { e ->
                val whenStr = stampFmt.format(Instant.ofEpochMilli(e.updatedAtEpochMillis))
                append("- id=${e.id} updated=$whenStr title=${e.title}\n")
                val body = e.body.trim().replace("\n", " ")
                val clipped = if (body.length <= maxBodyCharsPerEntry) {
                    body
                } else {
                    body.take(maxBodyCharsPerEntry) + "…"
                }
                append("  body: $clipped\n")
            }
        }.trimEnd()
    }
}
