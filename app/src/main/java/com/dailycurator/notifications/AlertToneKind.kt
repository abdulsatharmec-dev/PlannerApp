package com.dailycurator.notifications

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import java.security.MessageDigest

/** How a notification slot (reminders vs Pomodoro done) chooses audio. */
enum class AlertToneKind(val storageId: String) {
    /** Current system default notification sound (resolved when the channel is created). */
    DEFAULT_NOTIFICATION("default_notif"),

    /** Current system default alarm sound (usually easier to hear). */
    ALARM("alarm"),

    /** No sound and no vibration. */
    SILENT("silent"),

    /** Vibration only. */
    VIBRATE_ONLY("vibrate"),

    /** User-picked tone from the ringtone UI or an audio file (see stored URI). */
    CUSTOM("custom"),
    ;

    companion object {
        fun fromStorageId(id: String?): AlertToneKind =
            entries.firstOrNull { it.storageId == id } ?: DEFAULT_NOTIFICATION
    }
}

object AlertToneUriResolver {
    fun soundUri(context: Context, kind: AlertToneKind, customUriString: String): Uri? =
        when (kind) {
            AlertToneKind.SILENT, AlertToneKind.VIBRATE_ONLY -> null
            AlertToneKind.CUSTOM -> {
                val t = customUriString.trim()
                if (t.isEmpty()) null else runCatching { Uri.parse(t) }.getOrNull()
            }
            AlertToneKind.DEFAULT_NOTIFICATION ->
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            AlertToneKind.ALARM ->
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

    fun vibrates(kind: AlertToneKind): Boolean =
        when (kind) {
            AlertToneKind.SILENT -> false
            AlertToneKind.VIBRATE_ONLY -> true
            AlertToneKind.CUSTOM -> true
            else -> true
        }
}

object NotificationTonePack {
    fun packId(
        reminderKind: AlertToneKind,
        reminderCustomUri: String,
        pomoKind: AlertToneKind,
        pomoCustomUri: String,
    ): String {
        val raw =
            "${reminderKind.storageId}|${reminderCustomUri.trim()}|${pomoKind.storageId}|${pomoCustomUri.trim()}"
        val dig = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return dig.joinToString("") { b -> "%02x".format(b) }.take(12)
    }
}
