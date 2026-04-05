package com.dailycurator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailycurator.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): JournalEntryEntity?

    @Query(
        "SELECT * FROM journal_entries ORDER BY updatedAtEpochMillis DESC LIMIT :limit",
    )
    suspend fun getRecentForContext(limit: Int): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: JournalEntryEntity): Long

    @Update
    suspend fun update(entity: JournalEntryEntity)

    @Delete
    suspend fun delete(entity: JournalEntryEntity)
}
