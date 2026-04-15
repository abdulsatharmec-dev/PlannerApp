package com.dailycurator.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single [ExoPlayer] for home morning **local** clips so audio keeps playing when you scroll,
 * change tabs, or leave the screen (with [MorningMotivationPlaybackService]).
 */
@Singleton
class MorningMotivationLocalPlaybackHolder @Inject constructor(
    @ApplicationContext private val app: Context,
) {
    private val lock = Any()
    private var player: ExoPlayer? = null
    private var currentUri: String? = null
    private var playbackListener: Player.Listener? = null

    fun playerOrNull(): ExoPlayer? = synchronized(lock) { player }

    private fun acquireUnlocked(): ExoPlayer {
        var p = player
        if (p == null) {
            p = ExoPlayer.Builder(app).build().apply {
                setHandleAudioBecomingNoisy(true)
            }
            player = p
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    MorningMotivationPlaybackService.requestSync(app)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    MorningMotivationPlaybackService.requestSync(app)
                }
            }
            playbackListener = listener
            p.addListener(listener)
        }
        return p
    }

    fun attach(view: PlayerView, uri: String, autoplay: Boolean) {
        synchronized(lock) {
            val p = acquireUnlocked()
            if (currentUri != uri) {
                currentUri = uri
                p.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                p.prepare()
            }
            p.playWhenReady = autoplay
            view.player = p
        }
        MorningMotivationPlaybackService.requestSync(app)
    }

    fun detach(view: PlayerView) {
        synchronized(lock) {
            if (view.player === player) {
                view.player = null
            }
        }
        MorningMotivationPlaybackService.requestSync(app)
    }

    fun releaseAll() {
        synchronized(lock) {
            playbackListener?.let { l ->
                player?.removeListener(l)
            }
            playbackListener = null
            player?.release()
            player = null
            currentUri = null
            try {
                app.stopService(android.content.Intent(app, MorningMotivationPlaybackService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
