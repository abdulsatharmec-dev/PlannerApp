package com.dailycurator.ui.screens.pomodoro

import com.dailycurator.data.pomodoro.PomodoroLaunchRequest

data class PomodoroUiState(
    val launch: PomodoroLaunchRequest? = null,
    val plannedMinutes: Int = 25,
    val sessionTotalSeconds: Int? = null,
    val remainingSeconds: Int? = null,
    val running: Boolean = false,
    val sessionActive: Boolean = false,
)
