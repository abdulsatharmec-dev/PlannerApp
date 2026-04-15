package com.dailycurator.ui.screens.journal

import android.content.Context
import java.io.File
import java.util.UUID

object JournalVoiceFiles {
    private const val DIR = "journal_voice"

    fun voiceDir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    fun absoluteFile(context: Context, relativePath: String): File =
        File(context.filesDir, relativePath)

    fun newRelativePath(): String = "$DIR/${UUID.randomUUID()}.m4a"

    fun deleteIfExists(context: Context, relativePath: String?) {
        val p = relativePath?.trim().orEmpty()
        if (p.isEmpty()) return
        absoluteFile(context, p).delete()
    }
}
