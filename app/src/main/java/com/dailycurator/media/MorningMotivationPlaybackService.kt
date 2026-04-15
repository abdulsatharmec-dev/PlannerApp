package com.dailycurator.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dailycurator.MainActivity
import com.dailycurator.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val CHANNEL_ID = "morning_motivation_playback"
private const val NOTIFICATION_ID = 90421
private const val ACTION_SYNC = "com.dailycurator.media.MorningMotivationPlaybackService.SYNC"
private const val ACTION_PAUSE = "com.dailycurator.media.MorningMotivationPlaybackService.PAUSE"
private const val ACTION_PLAY = "com.dailycurator.media.MorningMotivationPlaybackService.PLAY"

/**
 * Foreground notification for morning **local** video audio while the UI is away from the player.
 */
@AndroidEntryPoint
class MorningMotivationPlaybackService : Service() {

    @Inject lateinit var holder: MorningMotivationLocalPlaybackHolder

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> holder.playerOrNull()?.pause()
            ACTION_PLAY -> holder.playerOrNull()?.run {
                playWhenReady = true
                play()
            }
        }
        updateForegroundState()
        return START_STICKY
    }

    private fun updateForegroundState() {
        val p = holder.playerOrNull()
        if (p == null) {
            stopForegroundSafely()
            stopSelf()
            return
        }
        val playing = p.isPlaying
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pausePi = servicePendingIntent(ACTION_PAUSE, 1)
        val playPi = servicePendingIntent(ACTION_PLAY, 2)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.morning_playback_notification_title))
            .setContentText(
                if (playing) {
                    getString(R.string.morning_playback_notification_playing)
                } else {
                    getString(R.string.morning_playback_notification_paused)
                },
            )
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
        if (playing) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.morning_playback_action_pause),
                pausePi,
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.morning_playback_action_resume),
                playPi,
            )
        }
        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, MorningMotivationPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun stopForegroundSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.morning_playback_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        mgr.createNotificationChannel(ch)
    }

    companion object {
        fun requestSync(context: Context) {
            val i = Intent(context, MorningMotivationPlaybackService::class.java).setAction(ACTION_SYNC)
            try {
                ContextCompat.startForegroundService(context, i)
            } catch (_: Exception) {
                context.startService(i)
            }
        }
    }
}
