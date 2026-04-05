package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.PomodoroDao
import com.dailycurator.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroRepository @Inject constructor(
    private val dao: PomodoroDao,
) {
    fun observeRecentSessions(limit: Int = 80): Flow<List<PomodoroSessionEntity>> =
        dao.observeRecent(limit)

    suspend fun insert(session: PomodoroSessionEntity): Long = dao.insert(session)

    suspend fun update(session: PomodoroSessionEntity) = dao.update(session)

    suspend fun sumFocusedForEntity(type: String, entityId: Long): Int =
        dao.sumFocusedSeconds(type, entityId)

    suspend fun completedCountForEntity(type: String, entityId: Long): Int =
        dao.countCompletedSessions(type, entityId)
}
