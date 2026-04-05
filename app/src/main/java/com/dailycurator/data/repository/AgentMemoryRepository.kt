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

    suspend fun extractFromRecentChat(messages: List<ChatMessageEntity>) {
        if (!prefs.isAgentMemoryEnabled()) return
        if (prefs.getCerebrasKey().isBlank()) return
        if (messages.size < 2) return
        val recent = messages.takeLast(8)
        val transcript = recent.joinToString("\n") { m ->
            val who = if (m.isUser) "User" else "Assistant"
            "$who: ${m.content}"
        }
        val existing = dao.getRecent(12).joinToString("\n") { it.content }
        val prompt = prefs.getMemoryExtractionPrompt()
        val userBlock = buildString {
            appendLine("EXISTING MEMORY LINES:")
            appendLine(if (existing.isBlank()) "(none)" else existing)
            appendLine()
            appendLine("RECENT CHAT:")
            appendLine(transcript)
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
        }.getOrNull() ?: return
        if (raw.trim().equals("NONE", ignoreCase = true)) return
        val lines = raw.lines()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .take(8)
        val now = System.currentTimeMillis()
        for (line in lines) {
            dao.insert(
                AgentMemoryEntity(
                    content = line,
                    updatedAtEpochMillis = now,
                    isManual = false,
                ),
            )
        }
        trimAutoIfHuge()
    }

    suspend fun ingestFromPlannerContext(plannerText: String) {
        if (!prefs.isAgentMemoryEnabled()) return
        if (prefs.getCerebrasKey().isBlank()) return
        val existing = dao.getRecent(12).joinToString("\n") { it.content }
        val prompt = prefs.getMemoryExtractionPrompt()
        val userBlock = buildString {
            appendLine("EXISTING MEMORY LINES:")
            appendLine(if (existing.isBlank()) "(none)" else existing)
            appendLine()
            appendLine("PLANNER SNAPSHOT (tasks / habits / goals):")
            appendLine(plannerText)
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
        }.getOrNull() ?: return
        if (raw.trim().equals("NONE", ignoreCase = true)) return
        val lines = raw.lines()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .take(10)
        val now = System.currentTimeMillis()
        for (line in lines) {
            dao.insert(
                AgentMemoryEntity(
                    content = line,
                    updatedAtEpochMillis = now,
                    isManual = false,
                ),
            )
        }
        trimAutoIfHuge()
    }

    private suspend fun trimAutoIfHuge() {
        while (dao.countAuto() > 80) {
            val auto = dao.getRecent(200).filter { !it.isManual }
            val oldest = auto.minByOrNull { it.updatedAtEpochMillis } ?: break
            dao.delete(oldest)
        }
    }
}
