package com.dailycurator.ui.screens.pomodoro

import androidx.lifecycle.ViewModel
import com.dailycurator.data.local.entity.PomodoroSessionEntity
import com.dailycurator.pomodoro.PomodoroTimerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val controller: PomodoroTimerController,
) : ViewModel() {

    val uiState: StateFlow<PomodoroUiState> = controller.uiState

    val recentSessions: StateFlow<List<PomodoroSessionEntity>> = controller.recentSessions

    fun onScreenResume() = controller.onScreenResume()

    fun setPlannedMinutes(minutes: Int) = controller.setPlannedMinutes(minutes)

    fun startOrResume() = controller.startOrResume()

    fun pause() = controller.pause()

    fun endSessionEarly() = controller.endSessionEarly()

    fun clearLinkedFocus() = controller.clearLinkedFocus()
}
