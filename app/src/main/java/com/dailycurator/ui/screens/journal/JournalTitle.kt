package com.dailycurator.ui.screens.journal

internal fun derivedTitle(rawTitle: String, body: String): String {
    val t = rawTitle.trim()
    if (t.isNotEmpty()) return t
    val firstLine = body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
    return when {
        firstLine == null -> "Untitled"
        firstLine.length <= 72 -> firstLine
        else -> firstLine.take(72) + "…"
    }
}
