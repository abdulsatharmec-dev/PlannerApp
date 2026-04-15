package com.dailycurator.ui.components

import androidx.compose.ui.graphics.Color

/** Preset ARGB colors for task tags (picker + defaults). */
object TaskTagUi {

    fun composeColorFromArgb(argb: Int): Color {
        val a = ((argb ushr 24) and 0xFF) / 255f
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return Color(r, g, b, a)
    }
    val presetArgbSwatches: List<Int> = listOf(
        0xFFE57373.toInt(),
        0xFFFFB74D.toInt(),
        0xFFFFF176.toInt(),
        0xFF81C784.toInt(),
        0xFF4FC3F7.toInt(),
        0xFF9575CD.toInt(),
        0xFFF06292.toInt(),
        0xFF90A4AE.toInt(),
    )

    private val suggestedDefaults: Map<String, Int> = mapOf(
        "Work" to 0xFF4FC3F7.toInt(),
        "Personal" to 0xFF81C784.toInt(),
        "Prayer" to 0xFF9575CD.toInt(),
    )

    fun argbForTag(tagName: String, stored: Map<String, Int>): Int {
        val key = tagName.trim()
        if (key.isEmpty()) return presetArgbSwatches[0]
        stored[key]?.let { return it }
        suggestedDefaults[key]?.let { return it }
        val idx = key.sumOf { it.code } and 0x7FFF
        return presetArgbSwatches[idx % presetArgbSwatches.size]
    }

    fun colorForTag(tagName: String, stored: Map<String, Int>): Color =
        composeColorFromArgb(argbForTag(tagName, stored))
}
