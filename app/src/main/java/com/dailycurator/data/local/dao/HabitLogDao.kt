package com.dailycurator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dailycurator.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {
    @Query(
        "SELECT * FROM habit_logs WHERE habitSeriesId = :seriesId ORDER BY loggedAtMillis DESC LIMIT :limit",
    )
    fun observeRecentForSeries(seriesId: String, limit: Int = 60): Flow<List<HabitLogEntity>>

    @Query(
        "SELECT * FROM habit_logs WHERE habitSeriesId = :seriesId ORDER BY loggedAtMillis DESC LIMIT :limit",
    )
    suspend fun getRecentForSeries(seriesId: String, limit: Int = 60): List<HabitLogEntity>

    @Query(
        """
        SELECT DISTINCT dayKey FROM habit_logs
        WHERE habitSeriesId = :seriesId AND dayKey >= :fromKey AND dayKey <= :toKey
        ORDER BY dayKey ASC
        """,
    )
    suspend fun getCompletedDayKeysInRange(seriesId: String, fromKey: String, toKey: String): List<String>

    @Query("SELECT DISTINCT dayKey FROM habit_logs WHERE habitSeriesId = :seriesId ORDER BY dayKey ASC")
    suspend fun getAllCompletedDayKeys(seriesId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitSeriesId = :seriesId")
    suspend fun deleteAllForSeries(seriesId: String)
}
