package com.dailycurator.data.local.dao

import androidx.room.*
import com.dailycurator.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY rank ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, rank ASC, startTime ASC",
    )
    fun getTasksBetween(startDate: String, endDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("UPDATE tasks SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("SELECT * FROM tasks WHERE isDone = 0")
    suspend fun getUndoneTasks(): List<TaskEntity>

    @Query(
        "SELECT * FROM tasks WHERE goalId = :goalId ORDER BY date DESC, rank ASC, startTime ASC",
    )
    fun getTasksForGoal(goalId: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET goalId = NULL WHERE goalId = :goalId")
    suspend fun clearGoalLinks(goalId: Long)

    @Query("SELECT * FROM tasks WHERE repeatSeriesId = :seriesId ORDER BY date ASC, rank ASC")
    suspend fun getTasksByRepeatSeriesId(seriesId: String): List<TaskEntity>

    /** Older repeats: rows with null repeatSeriesId sharing title + start/end times. */
    @Query(
        """
        SELECT * FROM tasks WHERE repeatSeriesId IS NULL
        AND title = :title AND startTime = :startTime AND endTime = :endTime
        ORDER BY date ASC, id ASC
        """,
    )
    suspend fun getTasksByLegacyRepeatFingerprint(
        title: String,
        startTime: String,
        endTime: String,
    ): List<TaskEntity>
}
