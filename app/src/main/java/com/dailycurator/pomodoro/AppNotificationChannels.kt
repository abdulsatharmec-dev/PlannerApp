package com.dailycurator.pomodoro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.notifications.AlertToneKind
import com.dailycurator.notifications.AlertToneUriResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences,
) {
    private val nm = context.getSystemService(NotificationManager::class.java)!!

    fun ensureAll() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        prefs.migrateLegacyAlertSoundIfNeeded()

        deleteLegacyStaticChannels()

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

        val packId = prefs.currentNotificationTonePackId()
        val last = prefs.getStoredNotificationTonePackId()
        if (last.isNotEmpty() && last != packId) {
            nm.safeDelete("dayroute_rem_$last")
            nm.safeDelete("dayroute_pom_$last")
        }

        val remId = "dayroute_rem_$packId"
        val pomId = "dayroute_pom_$packId"
        if (nm.getNotificationChannel(remId) == null) {
            val rk = prefs.getReminderToneKind()
            val ru = prefs.getReminderCustomToneUriString()
            val pk = prefs.getPomodoroToneKind()
            val pu = prefs.getPomodoroCustomToneUriString()

            createAlertChannel(
                id = remId,
                name = "Task & habit reminders",
                description = "Task and habit reminder alerts",
                kind = rk,
                customUri = ru,
            )
            createAlertChannel(
                id = pomId,
                name = "Pomodoro session finished",
                description = "When a focus session completes",
                kind = pk,
                customUri = pu,
            )
        }
        prefs.setStoredNotificationTonePackId(packId)
    }

    private fun NotificationManager.safeDelete(id: String) {
        try {
            deleteNotificationChannel(id)
        } catch (_: Exception) {
        }
    }

    private fun deleteLegacyStaticChannels() {
        listOf(
            "dayroute_reminders_v2",
            "dayroute_reminders",
            "pomodoro_complete",
            "dayroute_reminder_snd_default",
            "dayroute_reminder_snd_alarm",
            "dayroute_reminder_snd_silent",
            "dayroute_reminder_snd_vibrate",
            "dayroute_pomo_done_default",
            "dayroute_pomo_done_alarm",
            "dayroute_pomo_done_silent",
            "dayroute_pomo_done_vibrate",
        ).forEach { nm.safeDelete(it) }
    }

    private fun createAlertChannel(
        id: String,
        name: String,
        description: String,
        kind: AlertToneKind,
        customUri: String,
    ) {
        val soundUri = AlertToneUriResolver.soundUri(context, kind, customUri)
        val vibrate = AlertToneUriResolver.vibrates(kind)
        val importance = if (kind == AlertToneKind.SILENT) {
            NotificationManager.IMPORTANCE_DEFAULT
        } else {
            NotificationManager.IMPORTANCE_HIGH
        }
        val usage = if (kind == AlertToneKind.ALARM) {
            AudioAttributes.USAGE_ALARM
        } else {
            AudioAttributes.USAGE_NOTIFICATION
        }
        val attrs = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val vibPattern = longArrayOf(0, 280, 160, 280)
        nm.createNotificationChannel(
            NotificationChannel(id, name, importance).apply {
                this.description = description
                setShowBadge(false)
                if (soundUri != null) {
                    setSound(soundUri, attrs)
                    enableVibration(vibrate)
                    if (vibrate) vibrationPattern = vibPattern
                } else {
                    setSound(null, null)
                    enableVibration(vibrate)
                    if (vibrate) vibrationPattern = vibPattern
                }
            },
        )
    }
}
