package com.dailycurator.data.pomodoro

import javax.inject.Inject
import javax.inject.Singleton

data class PomodoroLaunchRequest(
    val entityType: String,
    val entityId: Long = 0L,
    val title: String,
    val habitSeriesId: String? = null,
    val plannedMinutes: Int = 25,
) {
    companion object {
        const val TYPE_FREE = "FREE"
        const val TYPE_TASK = "TASK"
        const val TYPE_GOAL = "GOAL"
        const val TYPE_HABIT = "HABIT"
    }
}

@Singleton
class PomodoroNavBridge @Inject constructor() {
    @Volatile
    private var pending: PomodoroLaunchRequest? = null

    @Synchronized
    fun push(request: PomodoroLaunchRequest) {
        pending = request
    }

    @Synchronized
    fun consume(): PomodoroLaunchRequest? {
        val p = pending
        pending = null
        return p
    }
}
