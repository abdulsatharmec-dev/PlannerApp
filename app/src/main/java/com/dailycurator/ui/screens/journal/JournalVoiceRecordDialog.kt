package com.dailycurator.ui.screens.journal

import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

private enum class VoiceRecordPhase {
    ReadyToRecord,
    Recording,
    Paused,
}

/**
 * Compact voice recorder at the top of the journal: read while recording.
 * Icon-only controls (mic / pause / play / stop / close).
 */
@Composable
fun JournalVoiceRecordOverlay(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onSaved: (relativePath: String) -> Unit,
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(VoiceRecordPhase.ReadyToRecord) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var pendingRelativePath by remember { mutableStateOf<String?>(null) }
    var displayElapsedMs by remember { mutableLongStateOf(0L) }
    var accumulatedMs by remember { mutableLongStateOf(0L) }
    var segmentStartElapsed by remember { mutableLongStateOf(0L) }

    fun discardPendingFileOnly() {
        JournalVoiceFiles.deleteIfExists(context, pendingRelativePath)
        pendingRelativePath = null
    }

    fun releaseRecorderQuietly() {
        val r = recorder
        recorder = null
        r?.run {
            try {
                stop()
            } catch (_: Throwable) {
            }
            try {
                reset()
            } catch (_: Throwable) {
            }
            try {
                release()
            } catch (_: Throwable) {
            }
        }
    }

    fun refreshDisplayedDuration() {
        displayElapsedMs = when (phase) {
            VoiceRecordPhase.ReadyToRecord -> 0L
            VoiceRecordPhase.Recording ->
                accumulatedMs + (SystemClock.elapsedRealtime() - segmentStartElapsed)
            VoiceRecordPhase.Paused -> accumulatedMs
        }
    }

    fun startRecordingFromReady() {
        val rel = JournalVoiceFiles.newRelativePath()
        val file = JournalVoiceFiles.absoluteFile(context, rel)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        val rec = createPreparedJournalVoiceRecorder(context, file.absolutePath)
        if (rec == null) {
            Toast.makeText(context, "Could not start recording.", Toast.LENGTH_SHORT).show()
            JournalVoiceFiles.deleteIfExists(context, rel)
            return
        }
        try {
            rec.start()
        } catch (_: Throwable) {
            try {
                rec.reset()
            } catch (_: Throwable) {
            }
            try {
                rec.release()
            } catch (_: Throwable) {
            }
            JournalVoiceFiles.deleteIfExists(context, rel)
            Toast.makeText(context, "Could not start recording.", Toast.LENGTH_SHORT).show()
            return
        }
        recorder = rec
        pendingRelativePath = rel
        accumulatedMs = 0L
        segmentStartElapsed = SystemClock.elapsedRealtime()
        phase = VoiceRecordPhase.Recording
        refreshDisplayedDuration()
    }

    fun pauseRecording() {
        val r = recorder ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            r.pause()
        } catch (_: Throwable) {
            return
        }
        accumulatedMs += SystemClock.elapsedRealtime() - segmentStartElapsed
        phase = VoiceRecordPhase.Paused
        refreshDisplayedDuration()
    }

    fun resumeRecording() {
        val r = recorder ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            r.resume()
        } catch (_: Throwable) {
            return
        }
        segmentStartElapsed = SystemClock.elapsedRealtime()
        phase = VoiceRecordPhase.Recording
    }

    fun stopAndSave() {
        val rel = pendingRelativePath
        releaseRecorderQuietly()
        phase = VoiceRecordPhase.ReadyToRecord
        if (rel == null) {
            onDismissRequest()
            return
        }
        val f = JournalVoiceFiles.absoluteFile(context, rel)
        val ok = f.exists() && f.length() > 512L
        if (ok) {
            onSaved(rel)
            pendingRelativePath = null
            displayElapsedMs = 0L
            accumulatedMs = 0L
            onDismissRequest()
        } else {
            JournalVoiceFiles.deleteIfExists(context, rel)
            pendingRelativePath = null
            Toast.makeText(context, "Recording too short.", Toast.LENGTH_SHORT).show()
            displayElapsedMs = 0L
            accumulatedMs = 0L
        }
    }

    fun discardAndClose() {
        releaseRecorderQuietly()
        discardPendingFileOnly()
        phase = VoiceRecordPhase.ReadyToRecord
        displayElapsedMs = 0L
        accumulatedMs = 0L
        onDismissRequest()
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseRecorderQuietly()
        }
    }

    val phaseRef = rememberUpdatedState(phase)
    val accumulatedRef = rememberUpdatedState(accumulatedMs)
    val segmentStartRef = rememberUpdatedState(segmentStartElapsed)
    LaunchedEffect(phase) {
        while (phaseRef.value == VoiceRecordPhase.Recording) {
            delay(100)
            displayElapsedMs =
                accumulatedRef.value + (SystemClock.elapsedRealtime() - segmentStartRef.value)
        }
    }

    val timeLabel = remember(displayElapsedMs) {
        val totalSec = (displayElapsedMs / 1000L).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        String.format(Locale.US, "%d:%02d", m, s)
    }

    val hint = when (phase) {
        VoiceRecordPhase.ReadyToRecord ->
            "Mic starts · ✓ saves · × closes"
        VoiceRecordPhase.Recording ->
            "Pause anytime · ✓ saves · × discards"
        VoiceRecordPhase.Paused ->
            "Play continues · ✓ saves · × discards"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.cardElevation(6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { discardAndClose() }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close recorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        timeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    when (phase) {
                        VoiceRecordPhase.ReadyToRecord -> {
                            IconButton(onClick = { startRecordingFromReady() }) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = "Start recording",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        VoiceRecordPhase.Recording -> {
                            IconButton(onClick = { pauseRecording() }) {
                                Icon(
                                    Icons.Filled.Pause,
                                    contentDescription = "Pause recording",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { stopAndSave() }) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Save voice note",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        VoiceRecordPhase.Paused -> {
                            IconButton(onClick = { resumeRecording() }) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Resume recording",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { stopAndSave() }) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Save voice note",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
