package com.dailycurator.pomodoro

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import com.dailycurator.di.ApplicationScope
import com.dailycurator.data.local.entity.PomodoroSessionEntity
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.PomodoroRepository
import com.dailycurator.ui.screens.pomodoro.PomodoroUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroTimerController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val repo: PomodoroRepository,
    private val bridge: PomodoroNavBridge,
) {
    private val nm = ContextCompat.getSystemService(appContext, NotificationManager::class.java)!!

    private val _ui = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _ui.asStateFlow()

    val recentSessions: StateFlow<List<PomodoroSessionEntity>> =
        repo.observeRecentSessions(100).stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    private var tickJob: Job? = null
    private var activeRow: PomodoroSessionEntity? = null

    fun onScreenResume() {
        val p = bridge.consume() ?: return
        if (_ui.value.sessionActive) return
        _ui.value = PomodoroUiState(
            launch = p,
            plannedMinutes = p.plannedMinutes.coerceIn(1, 180),
        )
    }

    fun setPlannedMinutes(minutes: Int) {
        if (_ui.value.sessionActive) return
        _ui.value = _ui.value.copy(plannedMinutes = minutes.coerceIn(1, 180))
    }

    fun startOrResume() {
        val s = _ui.value
        if (s.running) return
        if (s.sessionActive && s.remainingSeconds != null) {
            _ui.value = s.copy(running = true)
            startTicks()
            refreshNotification()
            return
        }
        if (!s.sessionActive) {
            scope.launch { startNewSession() }
        }
    }

    fun pause() {
        tickJob?.cancel()
        _ui.value = _ui.value.copy(running = false)
        refreshNotification()
    }

    fun endSessionEarly() = scope.launch {
        endSessionEarlyInternal()
    }

    fun endSessionEarlyFromAnywhere() {
        scope.launch { endSessionEarlyInternal() }
    }

    private suspend fun endSessionEarlyInternal() {
        tickJob?.cancel()
        _ui.value = _ui.value.copy(running = false)
        finishSession(completed = false)
    }

    fun clearLinkedFocus() = scope.launch {
        val mins = _ui.value.plannedMinutes
        if (_ui.value.sessionActive) {
            endSessionEarlyInternal()
        }
        _ui.value = PomodoroUiState(launch = null, plannedMinutes = mins)
        stopForegroundAndNotif()
    }

    private suspend fun startNewSession() {
        tickJob?.cancel()
        val s = _ui.value
        val req = s.launch
        val plannedSec = s.plannedMinutes * 60
        val entity = PomodoroSessionEntity(
            entityType = req?.entityType ?: PomodoroLaunchRequest.TYPE_FREE,
            entityId = req?.entityId ?: 0L,
            habitSeriesId = req?.habitSeriesId,
            title = req?.title?.takeIf { it.isNotBlank() } ?: "Focus session",
            startedAtMillis = System.currentTimeMillis(),
            endedAtMillis = null,
            plannedDurationSeconds = plannedSec,
            actualFocusedSeconds = 0,
            completed = false,
        )
        val id = repo.insert(entity)
        activeRow = entity.copy(id = id)
        _ui.value = s.copy(
            remainingSeconds = plannedSec,
            sessionTotalSeconds = plannedSec,
            running = true,
            sessionActive = true,
        )
        PomodoroForegroundService.start(appContext)
        refreshNotification()
        startTicks()
    }

    private fun elapsedSeconds(): Int {
        val row = activeRow ?: return 0
        val rem = _ui.value.remainingSeconds ?: row.plannedDurationSeconds
        return (row.plannedDurationSeconds - rem).coerceAtLeast(0)
    }

    private fun startTicks() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && _ui.value.running) {
                delay(1_000)
                if (!_ui.value.running) break
                val rem = (_ui.value.remainingSeconds ?: 0) - 1
                if (rem <= 0) {
                    _ui.value = _ui.value.copy(remainingSeconds = 0, running = false)
                    finishSession(completed = true)
                    break
                }
                _ui.value = _ui.value.copy(remainingSeconds = rem)
                refreshNotification()
            }
        }
    }

    private suspend fun finishSession(completed: Boolean) {
        val row = activeRow ?: return
        val actual = if (completed) row.plannedDurationSeconds else elapsedSeconds()
        repo.update(
            row.copy(
                endedAtMillis = System.currentTimeMillis(),
                actualFocusedSeconds = actual,
                completed = completed,
            ),
        )
        activeRow = null
        _ui.value = _ui.value.copy(
            remainingSeconds = null,
            sessionTotalSeconds = null,
            running = false,
            sessionActive = false,
        )
        stopForegroundAndNotif()
        if (completed) {
            showPomodoroCompleteNotification(row.title)
        }
    }

    private fun refreshNotification() {
        val state = _ui.value
        if (!state.sessionActive) return
        nm.notify(
            PomodoroNotificationIds.ONGOING_NOTIFICATION_ID,
            PomodoroNotificationHelper.buildOngoing(appContext, state),
        )
    }

    private fun stopForegroundAndNotif() {
        nm.cancel(PomodoroNotificationIds.ONGOING_NOTIFICATION_ID)
        PomodoroForegroundService.stop(appContext)
    }

    private fun showPomodoroCompleteNotification(title: String) {
        val open = android.app.PendingIntent.getActivity(
            appContext,
            99,
            android.content.Intent(appContext, com.dailycurator.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(com.dailycurator.MainActivity.EXTRA_OPEN_POMODORO, true)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = androidx.core.app.NotificationCompat.Builder(appContext, ReminderNotificationIds.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pomodoro complete")
            .setContentText(title)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(7102, notif)
    }
}
