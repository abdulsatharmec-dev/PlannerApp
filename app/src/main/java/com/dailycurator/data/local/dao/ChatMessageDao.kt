package com.dailycurator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dailycurator.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY createdAtEpochMillis ASC, id ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insert(entity: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
