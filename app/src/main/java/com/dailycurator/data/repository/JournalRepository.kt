package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.JournalDao
import com.dailycurator.data.local.entity.JournalEntryEntity
import com.dailycurator.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun JournalEntryEntity.toModel() = JournalEntry(
    id = id,
    title = title,
    body = body,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    title = title,
    body = body,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

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
}
