package com.dailycurator.data.chat

sealed class PendingChatDeletion {
    data class Task(val id: Long, val title: String) : PendingChatDeletion()
    data class Goal(val id: Long, val title: String) : PendingChatDeletion()
    data class Habit(val id: Long, val title: String) : PendingChatDeletion()
}
