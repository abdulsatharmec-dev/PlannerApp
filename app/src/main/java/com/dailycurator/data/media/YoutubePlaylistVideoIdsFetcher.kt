package com.dailycurator.data.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubePlaylistVideoIdsFetcher @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    private data class CacheEntry(val ids: List<String>, val fetchedAtMs: Long)

    private val cache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()
    private val ttlMs = TimeUnit.MINUTES.toMillis(45)

    suspend fun fetchOrderedVideoIds(playlistId: String, forceRefresh: Boolean = false): List<String> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceRefresh) {
                val c = cache[playlistId]
                if (c != null && now - c.fetchedAtMs < ttlMs) return@withContext c.ids
            }
            val ids = fetchFromNetwork(playlistId)
            cache[playlistId] = CacheEntry(ids, now)
            ids
        }

    private fun fetchFromNetwork(playlistId: String): List<String> {
        val url = "https://www.youtube.com/playlist?list=$playlistId"
        val req = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            )
            .build()
        return runCatching {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching emptyList()
                val html = resp.body.string()
                extractPlaylistVideoIds(html)
            }
        }.getOrElse { emptyList() }
    }

    companion object {
        internal fun extractPlaylistVideoIds(html: String): List<String> {
            val out = LinkedHashSet<String>()
            var i = 0
            while (true) {
                val idx = html.indexOf("playlistVideoRenderer", i)
                if (idx < 0) break
                val end = (idx + 2800).coerceAtMost(html.length)
                val window = html.substring(idx, end)
                val m = Regex("""\"videoId\":\"([\w-]{11})\"""").find(window)
                if (m != null) out.add(m.groupValues[1])
                i = idx + 1
            }
            return out.toList()
        }
    }
}
