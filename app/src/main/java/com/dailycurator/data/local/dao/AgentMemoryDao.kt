package com.dailycurator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailycurator.data.local.entity.AgentMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentMemoryDao {
    @Query("SELECT * FROM agent_memory ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<AgentMemoryEntity>>

    @Query("SELECT * FROM agent_memory ORDER BY updatedAtEpochMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AgentMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AgentMemoryEntity): Long

    @Update
    suspend fun update(entity: AgentMemoryEntity)

    @Delete
    suspend fun delete(entity: AgentMemoryEntity)

    @Query("DELETE FROM agent_memory WHERE isManual = 0")
    suspend fun deleteAllAuto()

    @Query("SELECT COUNT(*) FROM agent_memory WHERE isManual = 0")
    suspend fun countAuto(): Int
}
