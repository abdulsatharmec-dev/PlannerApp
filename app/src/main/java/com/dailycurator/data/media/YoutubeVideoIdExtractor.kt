package com.dailycurator.data.media

import android.net.Uri
import java.util.Locale
import java.util.regex.Pattern

/**
 * Pulls a YouTube watch id from a pasted URL or returns the string if it already looks like an id.
 * Handles `www.` / `m.` hosts, `v=` anywhere in the query, youtu.be, shorts, embed, and music.youtube.com.
 */
object YoutubeVideoIdExtractor {

    private val legacyPatterns = listOf(
        Pattern.compile("(?:youtube\\.com/watch\\?v=|youtube\\.com/embed/|youtube\\.com/v/)([\\w-]{11})"),
        Pattern.compile("youtu\\.be/([\\w-]{11})"),
        Pattern.compile("youtube\\.com/shorts/([\\w-]{11})"),
        Pattern.compile("m\\.youtube\\.com/watch\\?v=([\\w-]{11})"),
        Pattern.compile("(?:www\\.)?youtube\\.com/watch\\?v=([\\w-]{11})"),
    )

    private val pathIdRegex = Regex("""/(?:embed|e/|v|shorts)/([\w-]{11})(?:/|[?#]|$)""")

    private fun isVideoId(s: String): Boolean =
        s.length == 11 && s.all { it.isLetterOrDigit() || it == '-' || it == '_' }

    fun extract(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        if (isVideoId(s)) return s

        val forUri = when {
            s.startsWith("http://", ignoreCase = true) ||
                s.startsWith("https://", ignoreCase = true) -> s
            else -> "https://$s"
        }
        val uri = runCatching { Uri.parse(forUri) }.getOrNull()
        val host = uri?.host?.lowercase(Locale.US)
        if (uri != null && !host.isNullOrEmpty()) {
            when {
                host == "youtu.be" || host.endsWith(".youtu.be") -> {
                    val seg = uri.pathSegments.firstOrNull()?.substringBefore('?') ?: ""
                    if (isVideoId(seg)) return seg
                }
                host.contains("youtube") -> {
                    uri.getQueryParameter("v")?.trim()?.let { q ->
                        if (isVideoId(q)) return q
                    }
                    val path = uri.path ?: ""
                    pathIdRegex.find(path)?.groupValues?.getOrNull(1)?.let {
                        if (isVideoId(it)) return it
                    }
                }
            }
        }

        for (p in legacyPatterns) {
            val m = p.matcher(s)
            if (m.find()) return m.group(1)
        }
        return null
    }

    fun parseLines(block: String): List<String> =
        block.lines()
            .map { it.trim() }
            .mapNotNull { extract(it) }
            .distinct()
}
