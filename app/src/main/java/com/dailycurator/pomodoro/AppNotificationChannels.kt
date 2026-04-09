package com.dailycurator.pomodoro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
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
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(
                PomodoroNotificationIds.CHANNEL_ID,
                "Pomodoro timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows focus timer while running"
                setShowBadge(false)
                setSound(null, null)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                PomodoroNotificationIds.COMPLETE_CHANNEL_ID,
                "Pomodoro complete",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Sound when a focus session ends"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 280, 160, 280)
                setSound(soundUri, audioAttrs)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                ReminderNotificationIds.CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Task and habit reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 280, 160, 280)
                setSound(soundUri, audioAttrs)
            },
        )
    }
}

object ReminderNotificationIds {
    /** v2: explicit sound; new id so upgrades are not stuck on a silent channel. */
    const val CHANNEL_ID = "dayroute_reminders_v2"
    const val TASK_BASE_ID = 20_000
    const val HABIT_DAILY_ID = 31_000
}
