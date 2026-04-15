package com.dailycurator.ui.screens.journal

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlin.math.min

private data class VoiceCapturePreset(
    val channels: Int,
    val sampleRateHz: Int,
    val bitRate: Int,
)

/**
 * Tries mic sources and AAC presets (high → fallback) so more devices succeed [prepare].
 */
internal fun createPreparedJournalVoiceRecorder(
    context: Context,
    outputPath: String,
): MediaRecorder? {
    val sources = listOf(
        MediaRecorder.AudioSource.CAMCORDER,
        MediaRecorder.AudioSource.DEFAULT,
        MediaRecorder.AudioSource.MIC,
    )
    val presets = listOf(
        VoiceCapturePreset(channels = 2, sampleRateHz = 48_000, bitRate = 256_000),
        VoiceCapturePreset(channels = 1, sampleRateHz = 48_000, bitRate = 192_000),
        VoiceCapturePreset(channels = 1, sampleRateHz = 44_100, bitRate = 160_000),
        VoiceCapturePreset(channels = 1, sampleRateHz = 44_100, bitRate = 128_000),
    )
    for (source in sources) {
        for (preset in presets) {
            val recorder = newMediaRecorderInstance(context)
            try {
                configureForVoiceNote(recorder, outputPath, source, preset)
                recorder.prepare()
                return recorder
            } catch (_: Throwable) {
                try {
                    recorder.reset()
                } catch (_: Throwable) {
                }
                try {
                    recorder.release()
                } catch (_: Throwable) {
                }
            }
        }
    }
    return null
}

private fun newMediaRecorderInstance(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }

private fun configureForVoiceNote(
    recorder: MediaRecorder,
    outputPath: String,
    audioSource: Int,
    preset: VoiceCapturePreset,
) {
    recorder.setAudioSource(audioSource)
    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    recorder.setAudioEncodingBitRate(preset.bitRate)
    recorder.setAudioSamplingRate(preset.sampleRateHz)
    recorder.setAudioChannels(min(preset.channels, 2))
    recorder.setOutputFile(outputPath)
}
