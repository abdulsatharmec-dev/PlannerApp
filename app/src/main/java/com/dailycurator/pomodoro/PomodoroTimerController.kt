package com.dailycurator.pomodoro

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.dailycurator.di.ApplicationScope
import com.dailycurator.data.local.AppPreferences
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroTimerController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val repo: PomodoroRepository,
    private val bridge: PomodoroNavBridge,
    private val prefs: AppPreferences,
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

    /** Wall-clock instant when the current running phase should hit zero; 0 when paused or idle. */
    private var phaseEndWallClockMillis: Long = 0L

    fun onScreenResume() {
        syncWallClockIfRunning()
        val p = bridge.consume() ?: return
        if (_ui.value.sessionActive) return
        _ui.value = PomodoroUiState(
            launch = p,
            plannedMinutes = p.plannedMinutes.coerceIn(1, 180),
        )
    }

    /**
     * Task reminder "Pomodoro" action (and similar): [PomodoroScreen] is often not resumed when the
     * shade is used, so [onScreenResume] never consumes the bridge. Apply the launch here and start the timer.
     * Runs on [scope] (Main) so UI state is ready before the activity is brought to front.
     */
    suspend fun applyLaunchFromNotification(request: PomodoroLaunchRequest, autoStart: Boolean) {
        withContext(scope.coroutineContext) {
            syncWallClockIfRunning()
            if (_ui.value.sessionActive) return@withContext
            _ui.value = PomodoroUiState(
                launch = request,
                plannedMinutes = request.plannedMinutes.coerceIn(1, 180),
            )
            if (autoStart) {
                val s = _ui.value
                if (!s.running) {
                    if (!s.sessionActive) {
                        startNewSession()
                    } else if (s.remainingSeconds != null) {
                        _ui.value = s.copy(running = true)
                        startTicks()
                        refreshNotification()
                    }
                }
            }
        }
    }

    /**
     * Refreshes remaining time from the deadline (UI + notification catch-up after unlock / resume).
     */
    private fun syncWallClockIfRunning() {
        val s = _ui.value
        if (!s.running || !s.sessionActive || phaseEndWallClockMillis <= 0L) return
        val rem = remainingSecondsFromDeadline()
        if (rem <= 0) {
            scope.launch { completeDueToAlarm() }
            return
        }
        _ui.value = s.copy(remainingSeconds = rem)
        refreshNotification()
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
        val rem = if (phaseEndWallClockMillis > 0L) {
            remainingSecondsFromDeadline()
        } else {
            _ui.value.remainingSeconds ?: 0
        }
        cancelPhaseEndAlarm()
        phaseEndWallClockMillis = 0L
        _ui.value = _ui.value.copy(running = false, remainingSeconds = rem.coerceAtLeast(0))
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
        cancelPhaseEndAlarm()
        phaseEndWallClockMillis = 0L
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

    suspend fun completeDueToAlarm() {
        if (!_ui.value.sessionActive || !_ui.value.running) return
        tickJob?.cancel()
        tickJob = null
        cancelPhaseEndAlarm()
        phaseEndWallClockMillis = 0L
        _ui.value = _ui.value.copy(remainingSeconds = 0, running = false)
        finishSession(completed = true)
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
            val rem0 = _ui.value.remainingSeconds ?: 0
            if (rem0 <= 0) return@launch
            phaseEndWallClockMillis = System.currentTimeMillis() + rem0 * 1000L
            schedulePhaseEndAlarm(phaseEndWallClockMillis)
            while (isActive && _ui.value.running) {
                val rem = remainingSecondsFromDeadline()
                if (rem <= 0) {
                    cancelPhaseEndAlarm()
                    phaseEndWallClockMillis = 0L
                    _ui.value = _ui.value.copy(remainingSeconds = 0, running = false)
                    finishSession(completed = true)
                    break
                }
                _ui.value = _ui.value.copy(remainingSeconds = rem)
                refreshNotification()
                val msLeft = (phaseEndWallClockMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                delay(msLeft.coerceIn(250L..1_000L))
            }
        }
    }

    private fun remainingSecondsFromDeadline(): Int {
        if (phaseEndWallClockMillis <= 0L) return _ui.value.remainingSeconds ?: 0
        val msLeft = phaseEndWallClockMillis - System.currentTimeMillis()
        return ((msLeft + 999) / 1000).toInt().coerceAtLeast(0)
    }

    private fun phaseAlarmPendingIntent(): PendingIntent {
        val intent = Intent(appContext, PomodoroActionReceiver::class.java).apply {
            action = PomodoroNotificationActions.TIMER_PHASE_COMPLETE
        }
        return PendingIntent.getBroadcast(
            appContext,
            POMODORO_PHASE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun schedulePhaseEndAlarm(triggerMillis: Long) {
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = phaseAlarmPendingIntent()
        cancelPhaseEndAlarm()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerMillis, pi),
                    pi,
                )
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
            }
        } catch (_: SecurityException) {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    private fun cancelPhaseEndAlarm() {
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(phaseAlarmPendingIntent())
    }

    private suspend fun finishSession(completed: Boolean) {
        cancelPhaseEndAlarm()
        phaseEndWallClockMillis = 0L
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
        val channelId = prefs.pomodoroCompleteNotificationChannelId()
        val b = androidx.core.app.NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pomodoro complete")
            .setContentText(title)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_EVENT)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b.setDefaults(
                androidx.core.app.NotificationCompat.DEFAULT_SOUND or
                    androidx.core.app.NotificationCompat.DEFAULT_VIBRATE,
            )
        }
        nm.notify(7102, b.build())
    }

    companion object {
        private const val POMODORO_PHASE_ALARM_REQUEST_CODE = 7199
    }
}
