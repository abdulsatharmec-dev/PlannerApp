package com.dailycurator.pomodoro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nm = context.getSystemService(NotificationManager::class.java)!!

    fun ensureAll() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        nm.createNotificationChannel(
            NotificationChannel(
                PomodoroNotificationIds.CHANNEL_ID,
                "Pomodoro timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows focus timer while running"
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                ReminderNotificationIds.CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Task and habit reminders"
            },
        )
    }
}

object ReminderNotificationIds {
    const val CHANNEL_ID = "dayroute_reminders"
    const val TASK_BASE_ID = 20_000
    const val HABIT_DAILY_ID = 31_000
}
