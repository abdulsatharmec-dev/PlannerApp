package com.dailycurator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailycurator.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions ORDER BY startedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<PomodoroSessionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(actualFocusedSeconds), 0) FROM pomodoro_sessions
        WHERE completed = 1 AND entityType = :type AND (:entityId = 0 OR entityId = :entityId)
        """,
    )
    suspend fun sumFocusedSeconds(type: String, entityId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM pomodoro_sessions
        WHERE completed = 1 AND entityType = :type AND (:entityId = 0 OR entityId = :entityId)
        """,
    )
    suspend fun countCompletedSessions(type: String, entityId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PomodoroSessionEntity): Long

    @Update
    suspend fun update(session: PomodoroSessionEntity)
}
