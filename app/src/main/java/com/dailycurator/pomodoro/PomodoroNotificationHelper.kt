package com.dailycurator.pomodoro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dailycurator.MainActivity
import com.dailycurator.ui.screens.pomodoro.PomodoroUiState
import java.util.Locale
import java.util.concurrent.TimeUnit

object PomodoroNotificationHelper {

    fun buildOngoing(context: Context, state: PomodoroUiState): android.app.Notification {
        val total = state.sessionTotalSeconds ?: (state.plannedMinutes * 60)
        val rem = state.remainingSeconds ?: total
        val mm = TimeUnit.SECONDS.toMinutes(rem.toLong()).toInt()
        val ss = rem % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_POMODORO, true)
            },
            pendingFlags(),
        )

        val pausePi = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, PomodoroActionReceiver::class.java).setAction(PomodoroNotificationActions.PAUSE),
            pendingFlags(),
        )
        val resumePi = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, PomodoroActionReceiver::class.java).setAction(PomodoroNotificationActions.RESUME),
            pendingFlags(),
        )
        val stopPi = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, PomodoroActionReceiver::class.java).setAction(PomodoroNotificationActions.STOP),
            pendingFlags(),
        )

        val b = NotificationCompat.Builder(context, PomodoroNotificationIds.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Pomodoro · $timeStr")
            .setContentText(if (state.running) "Focusing…" else "Paused")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (state.running) {
            b.addAction(android.R.drawable.ic_media_pause, "Pause", pausePi)
        } else {
            b.addAction(android.R.drawable.ic_media_play, "Resume", resumePi)
        }
        b.addAction(android.R.drawable.ic_delete, "Stop", stopPi)

        return b.build()
    }

    private fun pendingFlags(): Int {
        var f = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            f = f or PendingIntent.FLAG_IMMUTABLE
        }
        return f
    }
}
