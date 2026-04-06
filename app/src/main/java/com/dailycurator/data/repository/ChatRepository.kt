package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.ChatMessageDao
import com.dailycurator.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(private val dao: ChatMessageDao) {

    fun observeMessages(): Flow<List<ChatMessageEntity>> = dao.observeMessages()

    suspend fun appendMessage(
        content: String,
        isUser: Boolean,
        totalTokens: Int? = null,
    ) {
        dao.insert(
            ChatMessageEntity(
                content = content,
                isUser = isUser,
                createdAtEpochMillis = System.currentTimeMillis(),
                totalTokens = totalTokens.takeIf { !isUser },
            ),
        )
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    /** Oldest-first, suitable for LLM context (last [limit] rows). */
    suspend fun getRecentAscending(limit: Int): List<ChatMessageEntity> =
        dao.getRecentDesc(limit).reversed()
}
