package com.dailycurator.pomodoro

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Holds the foreground notification slot while a Pomodoro session is active.
 * Timer logic lives in [PomodoroTimerController]; this service only calls [startForeground].
 */
@AndroidEntryPoint
class PomodoroForegroundService : Service() {

    @Inject lateinit var controller: PomodoroTimerController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = controller.uiState.value
        if (state.sessionActive) {
            val notif = PomodoroNotificationHelper.buildOngoing(this, state)
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(
                    this,
                    PomodoroNotificationIds.ONGOING_NOTIFICATION_ID,
                    notif,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(PomodoroNotificationIds.ONGOING_NOTIFICATION_ID, notif)
            }
        } else {
            stopForegroundService()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopForegroundService()
        super.onDestroy()
    }

    private fun stopForegroundService() {
        kotlin.runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
    }

    companion object {
        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PomodoroForegroundService::class.java),
            )
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, PomodoroForegroundService::class.java))
        }
    }
}
