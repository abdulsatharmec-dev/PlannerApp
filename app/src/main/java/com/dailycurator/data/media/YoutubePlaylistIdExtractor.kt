package com.dailycurator.data.media

import android.net.Uri
import java.util.Locale

object YoutubePlaylistIdExtractor {

    private val playlistIdRegex = Regex("""^PL[\w-]{10,}$""")

    fun isPlaylistId(s: String): Boolean = playlistIdRegex.matches(s.trim())

    /**
     * Returns the `list` playlist id from a youtube.com watch URL, or null if missing/invalid.
     */
    fun extractPlaylistIdFromWatchUrl(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val forUri = when {
            t.startsWith("http://", ignoreCase = true) ||
                t.startsWith("https://", ignoreCase = true) -> t
            else -> "https://$t"
        }
        val uri = runCatching { Uri.parse(forUri) }.getOrNull() ?: return null
        val host = uri.host?.lowercase(Locale.US) ?: return null
        if (!host.contains("youtube")) return null
        val list = uri.getQueryParameter("list")?.trim().orEmpty()
        return list.takeIf { isPlaylistId(it) }
    }
}
