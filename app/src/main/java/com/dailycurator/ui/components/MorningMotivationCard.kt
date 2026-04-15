package com.dailycurator.ui.components

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.dailycurator.R
import com.dailycurator.data.media.MorningClip
import com.dailycurator.di.MorningMotivationPlaybackEntryPoint
import com.dailycurator.media.MorningMotivationLocalPlaybackHolder
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dagger.hilt.android.EntryPointAccessors
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private class LocalVideoViewTag(
    val holder: MorningMotivationLocalPlaybackHolder,
    var fullscreenDialog: Dialog?,
)

private fun fullscreenToolbarButton(context: Context, label: String): Button =
    Button(context).apply {
        text = label
        setTextColor(0xFFFFFFFF.toInt())
        setTypeface(null, Typeface.BOLD)
        setBackgroundColor(0x88000000.toInt())
        val p = (10 * context.resources.displayMetrics.density).toInt().coerceAtLeast(6)
        setPadding(p, p / 2, p, p / 2)
    }

/**
 * Fullscreen chrome: rotate video to use the full dialog (no activity rotation), touch lock, close.
 */
private fun buildFullscreenVideoRoot(
    context: Context,
    videoView: View,
    onCloseFullscreen: () -> Unit,
): FrameLayout {
    val root = FrameLayout(context)
    (videoView.parent as? ViewGroup)?.removeView(videoView)
    root.addView(
        videoView,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )

    var rotated = false
    var touchLocked = false

    val blocker = View(context).apply {
        visibility = View.GONE
        isClickable = true
        isFocusable = true
    }
    root.addView(
        blocker,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )

    fun syncBlocker() {
        blocker.visibility = if (touchLocked) View.VISIBLE else View.GONE
    }

    val d = context.resources.displayMetrics.density
    val toolbar = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(0xCC000000.toInt())
        val pad = (8 * d).toInt().coerceAtLeast(4)
        setPadding(pad, pad, pad, pad)
    }

    val btnRotate = fullscreenToolbarButton(context, context.getString(R.string.fullscreen_video_rotate))
    val btnLock = fullscreenToolbarButton(context, context.getString(R.string.fullscreen_touch_lock))
    val btnClose = fullscreenToolbarButton(context, context.getString(R.string.fullscreen_close))

    btnRotate.setOnClickListener {
        rotated = !rotated
        applyFullscreenVideoRotation(videoView, root, rotated)
        btnRotate.text = context.getString(
            if (rotated) R.string.fullscreen_video_rotate_undo else R.string.fullscreen_video_rotate,
        )
    }
    btnLock.setOnClickListener {
        touchLocked = !touchLocked
        syncBlocker()
        btnLock.text = context.getString(
            if (touchLocked) R.string.fullscreen_touch_unlock else R.string.fullscreen_touch_lock,
        )
    }
    btnClose.setOnClickListener { onCloseFullscreen() }

    toolbar.addView(btnRotate)
    toolbar.addView(
        btnLock,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = (8 * d).toInt() },
    )
    toolbar.addView(
        btnClose,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = (8 * d).toInt() },
    )

    val tbLp = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.END or Gravity.TOP,
    ).apply {
        topMargin = (40 * d).toInt()
        marginEnd = (8 * d).toInt()
    }
    root.addView(toolbar, tbLp)
    return root
}

/** Rotate only the video surface so landscape content can fill the device without rotating the app. */
private fun applyFullscreenVideoRotation(video: View, root: FrameLayout, rotated: Boolean) {
    root.post {
        val rw = root.width.toFloat()
        val rh = root.height.toFloat()
        if (rw <= 0f || rh <= 0f) return@post
        val lp = (video.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).also { video.layoutParams = it }
        lp.width = FrameLayout.LayoutParams.MATCH_PARENT
        lp.height = FrameLayout.LayoutParams.MATCH_PARENT
        lp.gravity = Gravity.CENTER
        video.layoutParams = lp
        video.pivotX = rw / 2f
        video.pivotY = rh / 2f
        if (!rotated) {
            video.rotation = 0f
            video.translationX = 0f
            video.translationY = 0f
            video.scaleX = 1f
            video.scaleY = 1f
        } else {
            video.rotation = 90f
            val scale = max(rw, rh) / min(rw, rh)
            video.scaleX = scale
            video.scaleY = scale
            video.translationX = 0f
            video.translationY = 0f
        }
        root.requestLayout()
    }
}

private fun resetFullscreenVideoTransform(video: View, root: FrameLayout) {
    applyFullscreenVideoRotation(video, root, false)
}

/** Picks a random index; if [avoidIndex] is set and there is more than one clip, tries not to repeat it. */
private fun randomClipIndex(clips: List<MorningClip>, avoidIndex: Int? = null): Int {
    if (clips.isEmpty()) return 0
    if (clips.size == 1) return 0
    var idx = clips.indices.random()
    if (avoidIndex == null) return idx
    var attempts = 0
    while (idx == avoidIndex && attempts++ < 48) {
        idx = clips.indices.random()
    }
    return idx
}

@Composable
fun MorningMotivationCard(
    cardTitle: String = "Motivation",
    clips: List<MorningClip>,
    morningMotivationAutoplay: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (clips.isEmpty()) return

    val context = LocalContext.current
    val playbackEntry = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MorningMotivationPlaybackEntryPoint::class.java,
        )
    }

    val clipsIdentity = remember(clips) {
        clips.joinToString("\u0000") { "${it.id}|${it.youtubeVideoId}|${it.localUri}" }
    }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(clipsIdentity) {
        if (clips.isEmpty()) return@LaunchedEffect
        selectedIndex = selectedIndex.coerceIn(0, clips.lastIndex)
    }

    val clip = clips[selectedIndex.coerceIn(0, clips.lastIndex)]

    LaunchedEffect(clip.isYoutube, clip.id) {
        if (clip.isYoutube) {
            playbackEntry.morningMotivationLocalPlaybackHolder().releaseAll()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(4.dp))
                    Column {
                        Text(
                            cardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            clip.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        selectedIndex = randomClipIndex(clips, selectedIndex)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text("Another clip")
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 12.dp),
            ) {
                if (clip.isYoutube && clip.youtubeVideoId != null) {
                    key(clip.id, clip.youtubeVideoId, morningMotivationAutoplay) {
                        YoutubeMorningPlayer(
                            videoId = clip.youtubeVideoId,
                            autoplay = morningMotivationAutoplay,
                        )
                    }
                } else if (clip.localUri != null) {
                    key(clip.id, clip.localUri, morningMotivationAutoplay) {
                        LocalVideoPlayer(
                            uriString = clip.localUri,
                            autoplay = morningMotivationAutoplay,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YoutubeMorningPlayer(videoId: String, autoplay: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var touchLocked by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        AndroidView(
            factory = { context ->
                YouTubePlayerView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    lifecycleOwner.lifecycle.addObserver(this)
                    enableAutomaticInitialization = false

                    var fullscreenDialog: Dialog? = null
                    addFullscreenListener(
                        object : FullscreenListener {
                            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                                var dialogRef: Dialog? = null
                                val root = buildFullscreenVideoRoot(context, fullscreenView) {
                                    dialogRef?.dismiss()
                                }
                                dialogRef = Dialog(
                                    context,
                                    android.R.style.Theme_Black_NoTitleBar_Fullscreen,
                                ).apply {
                                    setContentView(root)
                                    setOnDismissListener {
                                        resetFullscreenVideoTransform(fullscreenView, root)
                                        exitFullscreen()
                                        fullscreenDialog = null
                                    }
                                    show()
                                }
                                fullscreenDialog = dialogRef
                            }

                            override fun onExitFullscreen() {
                                fullscreenDialog?.dismiss()
                                fullscreenDialog = null
                            }
                        },
                    )

                    val embedderOrigin = "https://${context.packageName.lowercase(Locale.US)}"
                    val options = IFramePlayerOptions.Builder()
                        .origin(embedderOrigin)
                        .controls(1)
                        .fullscreen(1)
                        .autoplay(if (autoplay) 1 else 0)
                        .build()

                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                if (autoplay) {
                                    youTubePlayer.loadVideo(videoId, 0f)
                                } else {
                                    youTubePlayer.cueVideo(videoId, 0f)
                                }
                            }
                        },
                        true,
                        options,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { view ->
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            },
        )
        if (touchLocked) {
            AndroidView(
                factory = { View(it).apply { setOnTouchListener { _, _ -> true } } },
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(
            onClick = { touchLocked = !touchLocked },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            Icon(
                imageVector = if (touchLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = if (touchLocked) {
                    stringResource(R.string.embedded_touch_unlock)
                } else {
                    stringResource(R.string.embedded_touch_lock)
                },
            )
        }
    }
}

@Composable
private fun LocalVideoPlayer(uriString: String, autoplay: Boolean) {
    var touchLocked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val holder = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MorningMotivationPlaybackEntryPoint::class.java,
        ).morningMotivationLocalPlaybackHolder()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        AndroidView(
            factory = { ctx ->
                lateinit var embedded: PlayerView
                embedded = PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    useController = true
                    val tagObj = LocalVideoViewTag(holder, null)
                    tag = tagObj
                    setFullscreenButtonClickListener { isFullscreen ->
                        val t = tag as LocalVideoViewTag
                        val h = t.holder
                        if (isFullscreen) {
                            val wasPlaying = h.playerOrNull()?.let { p ->
                                p.isPlaying || p.playWhenReady
                            } == true
                            h.detach(embedded)
                            val fullView = PlayerView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                useController = true
                            }
                            h.attach(fullView, uriString, wasPlaying || autoplay)
                            var dialogRef: Dialog? = null
                            val root = buildFullscreenVideoRoot(ctx, fullView) {
                                dialogRef?.dismiss()
                            }
                            dialogRef = Dialog(
                                ctx,
                                android.R.style.Theme_Black_NoTitleBar_Fullscreen,
                            ).apply {
                                setContentView(root)
                                setOnDismissListener {
                                    resetFullscreenVideoTransform(fullView, root)
                                    h.detach(fullView)
                                    h.attach(
                                        embedded,
                                        uriString,
                                        h.playerOrNull()?.playWhenReady == true,
                                    )
                                    t.fullscreenDialog = null
                                }
                                show()
                            }
                            t.fullscreenDialog = dialogRef
                        } else {
                            t.fullscreenDialog?.dismiss()
                        }
                    }
                }
                holder.attach(embedded, uriString, autoplay)
                embedded
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val pv = view as? PlayerView ?: return@AndroidView
                val t = pv.tag as? LocalVideoViewTag ?: return@AndroidView
                t.holder.attach(pv, uriString, autoplay)
            },
            onRelease = { view ->
                val pv = view as? PlayerView ?: return@AndroidView
                val t = pv.tag as? LocalVideoViewTag ?: return@AndroidView
                t.fullscreenDialog?.dismiss()
                t.holder.detach(pv)
            },
        )
        if (touchLocked) {
            AndroidView(
                factory = { View(it).apply { setOnTouchListener { _, _ -> true } } },
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(
            onClick = { touchLocked = !touchLocked },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            Icon(
                imageVector = if (touchLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = if (touchLocked) {
                    stringResource(R.string.embedded_touch_unlock)
                } else {
                    stringResource(R.string.embedded_touch_lock)
                },
            )
        }
    }
}
