package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.JournalDao
import com.dailycurator.data.local.entity.JournalEntryEntity
import com.dailycurator.data.model.JournalAiChannel
import com.dailycurator.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private fun JournalEntryEntity.toModel() = JournalEntry(
    id = id,
    title = title,
    body = body,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    includeInAgentChat = includeInAgentChat,
    includeInAssistantInsight = includeInAssistantInsight,
    includeInWeeklyGoalsInsight = includeInWeeklyGoalsInsight,
    isEvergreen = isEvergreen,
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    title = title,
    body = body,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    includeInAgentChat = includeInAgentChat,
    includeInAssistantInsight = includeInAssistantInsight,
    includeInWeeklyGoalsInsight = includeInWeeklyGoalsInsight,
    isEvergreen = isEvergreen,
)

private fun JournalEntry.localDay(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(updatedAtEpochMillis).atZone(zone).toLocalDate()

private fun JournalEntry.passesChannel(channel: JournalAiChannel): Boolean = when (channel) {
    JournalAiChannel.AgentChat -> includeInAgentChat
    JournalAiChannel.AssistantInsight -> includeInAssistantInsight
    JournalAiChannel.WeeklyGoalsInsight -> includeInWeeklyGoalsInsight
}

@Singleton
class JournalRepository @Inject constructor(
    private val dao: JournalDao,
) {
    fun observeAll(): Flow<List<JournalEntry>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun insert(entry: JournalEntry): Long = dao.insert(entry.toEntity())

    suspend fun update(entry: JournalEntry) = dao.update(entry.toEntity())

    suspend fun delete(entry: JournalEntry) = dao.delete(entry.toEntity())

    suspend fun getById(id: Long): JournalEntry? = dao.getById(id)?.toModel()

    suspend fun getRecentForAiContext(limit: Int = 35): List<JournalEntry> =
        dao.getRecentForContext(limit).map { it.toModel() }

    /**
     * Entries whose local calendar day (from [JournalEntry.updatedAtEpochMillis]) falls in
     * `[referenceDate - (windowDays-1), reference]`, sorted recent-first, respecting per-entry flags.
     */
    suspend fun getJournalForAiChannel(
        channel: JournalAiChannel,
        referenceDate: LocalDate,
        windowDays: Int,
        zone: ZoneId,
        maxEntries: Int,
    ): List<JournalEntry> {
        val win = windowDays.coerceIn(1, 30)
        val start = referenceDate.minusDays((win - 1).toLong())
        val fetch = (maxEntries * 4).coerceAtLeast(48)
        return dao.getRecentForContext(fetch)
            .asSequence()
            .map { it.toModel() }
            .filter { it.passesChannel(channel) }
            .filter {
                val d = it.localDay(zone)
                !d.isAfter(referenceDate) && !d.isBefore(start)
            }
            .take(maxEntries)
            .toList()
    }
}
