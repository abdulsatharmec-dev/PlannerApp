package com.dailycurator.data.local.dao

import androidx.room.*
import com.dailycurator.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE date = :date")
    fun getHabitsForDate(date: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HabitEntity?

    @Query(
        """
        SELECT * FROM habits WHERE
          (seriesId = :seriesId AND seriesId != '')
          OR (CAST(id AS TEXT) = :seriesId AND seriesId = '')
        ORDER BY date DESC, id DESC LIMIT 1
        """,
    )
    suspend fun getLatestForSeries(seriesId: String): HabitEntity?

    @Query(
        """
        SELECT * FROM habits WHERE
          (seriesId = :seriesId AND seriesId != '')
          OR (CAST(id AS TEXT) = :seriesId AND seriesId = '')
        """,
    )
    suspend fun getAllForSeries(seriesId: String): List<HabitEntity>

    @Query(
        """
        DELETE FROM habits WHERE
          (seriesId = :seriesId AND seriesId != '')
          OR (CAST(id AS TEXT) = :seriesId AND seriesId = '')
        """,
    )
    suspend fun deleteAllForSeries(seriesId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)
}
