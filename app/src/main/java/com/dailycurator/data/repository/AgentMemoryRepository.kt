package com.dailycurator.data.repository

import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.dao.AgentMemoryDao
import com.dailycurator.data.local.entity.AgentMemoryEntity
import com.dailycurator.data.local.entity.ChatMessageEntity
import com.dailycurator.data.remote.CerebrasChatMessage
import com.dailycurator.data.remote.CerebrasRestClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentMemoryRepository @Inject constructor(
    private val dao: AgentMemoryDao,
    private val cerebras: CerebrasRestClient,
    private val prefs: AppPreferences,
) {

    fun observeAll(): Flow<List<AgentMemoryEntity>> = dao.observeAll()

    suspend fun insertManual(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        dao.insert(
            AgentMemoryEntity(
                content = trimmed,
                updatedAtEpochMillis = System.currentTimeMillis(),
                isManual = true,
            ),
        )
    }

    suspend fun updateEntry(entity: AgentMemoryEntity) {
        dao.update(entity.copy(updatedAtEpochMillis = System.currentTimeMillis()))
    }

    /** User edits in the Memory UI always mark the row as manual. */
    suspend fun saveUserEdit(entity: AgentMemoryEntity, newContent: String) {
        val trimmed = newContent.trim()
        if (trimmed.isEmpty()) return
        dao.update(
            entity.copy(
                content = trimmed,
                updatedAtEpochMillis = System.currentTimeMillis(),
                isManual = true,
            ),
        )
    }

    suspend fun delete(entity: AgentMemoryEntity) {
        dao.delete(entity)
    }

    suspend fun formatForChatContext(maxEntries: Int = 24): String {
        if (!prefs.isAgentMemoryEnabled()) return ""
        val rows = dao.getRecent(maxEntries)
        if (rows.isEmpty()) return ""
        return buildString {
            appendLine("--- LONG-TERM MEMORY (user-editable; treat as factual preferences about the user) ---")
            rows.forEach { appendLine("- ${it.content}") }
        }
    }

    /**
     * Suggests memory lines from **user chat messages only** (no assistant text, no planner).
     * Does not write to the database — caller shows UI confirmation before [insertUserConfirmedMemoryLines].
     */
    suspend fun proposeMemoryFromUserChatOnly(messages: List<ChatMessageEntity>): List<String> {
        if (!prefs.isAgentMemoryEnabled()) return emptyList()
        if (messages.isEmpty()) return emptyList()
        val window = messages.takeLast(16)
        val userMsgs = window.filter { it.isUser }
        if (userMsgs.isEmpty()) return emptyList()

        val existingRowsEarly = dao.getRecent(40).map { it.content }
        if (prefs.getCerebrasKey().isBlank()) {
            return mergeProposalCandidates(
                emptyList(),
                volunteeredProfileLinesFromUserMessages(userMsgs),
                existingRowsEarly,
            )
        }

        val userTranscript = userMsgs.mapIndexed { i, m ->
            "${i + 1}. ${m.content.trim()}"
        }.joinToString("\n")

        val existing = dao.getRecent(12).joinToString("\n") { it.content }
        val prompt = prefs.getMemoryExtractionPrompt()
        val userBlock = buildString {
            appendLine("EXISTING MEMORY LINES:")
            appendLine(if (existing.isBlank()) "(none)" else existing)
            appendLine()
            appendLine("USER MESSAGES ONLY (extract only from these; ignore anything not listed):")
            appendLine(userTranscript)
        }
        val raw = runCatching {
            cerebras.completePlainText(
                messages = listOf(
                    CerebrasChatMessage(role = "system", content = prompt),
                    CerebrasChatMessage(role = "user", content = userBlock),
                ),
                temperature = 0.2,
                maxTokens = 1024,
            )
        }.getOrNull()
        if (raw == null) {
            val fb = volunteeredProfileLinesFromUserMessages(userMsgs)
            val existingRows = dao.getRecent(40).map { it.content }
            return mergeProposalCandidates(emptyList(), fb, existingRows)
        }

        val fromModel = parseMemoryBulletLines(raw)
        val fromVolunteered = volunteeredProfileLinesFromUserMessages(userMsgs)
        val existingRows = dao.getRecent(40).map { it.content }
        return mergeProposalCandidates(fromModel, fromVolunteered, existingRows)
    }

    /** Persists lines the user approved in the chat memory dialog (stored as manual). */
    suspend fun insertUserConfirmedMemoryLines(lines: List<String>) {
        if (!prefs.isAgentMemoryEnabled()) return
        val existingRows = dao.getRecent(40).map { it.content }.toMutableList()
        val now = System.currentTimeMillis()
        for (line in lines) {
            val t = line.trim().removePrefix("-").trim()
            if (t.length < 6) continue
            if (isDuplicateMemoryLine(t, existingRows)) continue
            dao.insert(
                AgentMemoryEntity(
                    content = t,
                    updatedAtEpochMillis = now,
                    isManual = true,
                ),
            )
            existingRows.add(t)
        }
    }

    private companion object {
        private val rxIAmNameAge = Regex(
            """(?i)\b(?:i\s*'m|i\s+am)\s+([a-z][a-z\s.'-]+)\s+age\s+(\d{1,3})\b""",
        )
        private val rxIAmCommaAge = Regex(
            """(?i)\b(?:i\s*'m|i\s+am)\s+([^,\n]{2,42}),\s*age\s+(\d{1,3})\b""",
        )
        private val rxMyNameIs = Regex(
            """(?i)my\s+name\s+is\s+([a-z][a-z\s.'-]{1,48})(?:\s*[,.!?\n]|$)""",
        )

        private fun mergeProposalCandidates(
            fromModel: List<String>,
            fromVolunteered: List<String>,
            existingContents: List<String>,
        ): List<String> {
            return (fromModel + fromVolunteered)
                .map { it.trim().removePrefix("-").trim() }
                .filter { it.length >= 6 }
                .distinctBy { it.lowercase() }
                .filter { !isDuplicateMemoryLine(it, existingContents) }
                .take(8)
        }

        private fun parseMemoryBulletLines(raw: String): List<String> {
            var s = raw.trim()
            if (s.equals("NONE", ignoreCase = true)) return emptyList()
            if (s.startsWith("```")) {
                s = s.removePrefix("```").trim()
                val firstNl = s.indexOf('\n')
                if (firstNl >= 0) {
                    val first = s.substring(0, firstNl).trim().lowercase()
                    if (first == "text" || first == "plaintext" || first == "plain") {
                        s = s.substring(firstNl + 1).trim()
                    }
                }
                s = s.removeSuffix("```").trim()
            }
            return s.lines()
                .map { line ->
                    line.trim()
                        .removePrefix("-")
                        .removePrefix("*")
                        .trim()
                        .replaceFirst(Regex("""^\d+\.\s*"""), "")
                        .trim()
                }
                .filter { it.length >= 8 }
                .filterNot { isLikelyPreambleLine(it) }
        }

        private fun isLikelyPreambleLine(line: String): Boolean {
            val l = line.lowercase()
            return l.startsWith("here ") || l.startsWith("sure") || l.startsWith("okay") ||
                l.startsWith("ok,") || l.startsWith("below") || l.startsWith("the user") && l.contains("following")
        }

        private fun volunteeredProfileLinesFromUserMessages(messages: List<ChatMessageEntity>): List<String> {
            val ordered = LinkedHashSet<String>()
            for (m in messages) {
                if (!m.isUser) continue
                val t = m.content
                rxIAmNameAge.find(t)?.let { match ->
                    val name = match.groupValues[1].trim().replace(Regex("""\s+"""), " ")
                    val age = match.groupValues[2]
                    if (name.length >= 2) {
                        ordered.add("User's name is $name; they said they are $age years old.")
                    }
                }
                rxIAmCommaAge.find(t)?.let { match ->
                    val name = match.groupValues[1].trim().replace(Regex("""\s+"""), " ")
                    val age = match.groupValues[2]
                    if (name.length >= 2) {
                        ordered.add("User's name is $name; they said they are $age years old.")
                    }
                }
                rxMyNameIs.find(t)?.let { match ->
                    val name = match.groupValues[1].trim().replace(Regex("""\s+"""), " ")
                    if (name.length >= 2) {
                        ordered.add("User's name is $name (they said so in chat).")
                    }
                }
            }
            return ordered.toList()
        }

        private fun isDuplicateMemoryLine(line: String, existing: List<String>): Boolean {
            val n = line.lowercase()
            return existing.any { existingLine ->
                val o = existingLine.lowercase()
                when {
                    o == n -> true
                    o.length >= 12 && n.contains(o) -> true
                    n.length >= 12 && o.contains(n) -> true
                    else -> false
                }
            }
        }
    }
}
