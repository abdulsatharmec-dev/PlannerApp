package com.dailycurator.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Stable pseudo-random accent per task id (distinct hues on the timeline). */
fun scheduleTaskAccentColor(id: Long): Color {
    val golden = 0.618033988749895
    val h = abs((id.toDouble() * golden) % 1.0).toFloat()
    return Color.hsv(h * 360f, 0.58f, 0.82f)
}
