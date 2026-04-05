package com.dailycurator.data.gmail

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GmailListItem(val id: String, val threadId: String?)

data class GmailMessageDigest(
    val id: String,
    val snippet: String,
    val from: String,
    val subject: String,
    val date: String,
)

@Singleton
class GmailRestClient @Inject constructor(private val gson: Gson) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun listMessageIds(accessToken: String, query: String, maxResults: Int): List<GmailListItem> =
        withContext(Dispatchers.IO) {
            val qEnc = URLEncoder.encode(query, Charsets.UTF_8.name())
            val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=$qEnc&maxResults=$maxResults"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Gmail list HTTP ${resp.code}: $body")
                val root = gson.fromJson(body, JsonObject::class.java) ?: return@withContext emptyList()
                val arr = root.getAsJsonArray("messages") ?: return@withContext emptyList()
                arr.mapNotNull { el ->
                    if (!el.isJsonObject) return@mapNotNull null
                    val o = el.asJsonObject
                    val id = o.get("id")?.asString ?: return@mapNotNull null
                    GmailListItem(id = id, threadId = o.get("threadId")?.asString)
                }
            }
        }

    suspend fun getMessageDigest(accessToken: String, messageId: String): GmailMessageDigest =
        withContext(Dispatchers.IO) {
            val url =
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId" +
                    "?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Gmail get HTTP ${resp.code}: $body")
                val root = gson.fromJson(body, JsonObject::class.java) ?: error("Invalid message JSON")
                parseDigest(root, messageId)
            }
        }

    suspend fun getMessagePlainText(accessToken: String, messageId: String, maxChars: Int = 12_000): String =
        withContext(Dispatchers.IO) {
            val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId?format=full"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Gmail full HTTP ${resp.code}: $body")
                val root = gson.fromJson(body, JsonObject::class.java) ?: return@withContext ""
                val plain = extractPlainFromPayload(root.getAsJsonObject("payload"))
                if (plain.length <= maxChars) plain else plain.take(maxChars) + "\n…(truncated)"
            }
        }

    suspend fun sendRawRfc822(accessToken: String, rawMimeUtf8: String) = withContext(Dispatchers.IO) {
        val raw = Base64.encodeToString(
            rawMimeUtf8.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
        val json = """{"raw":"$raw"}"""
        val req = Request.Builder()
            .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Gmail send HTTP ${resp.code}: $body")
        }
    }

    private fun parseDigest(root: JsonObject, fallbackId: String): GmailMessageDigest {
        val id = root.get("id")?.asString ?: fallbackId
        val snippet = root.get("snippet")?.asString ?: ""
        var subject = ""
        var from = ""
        var date = ""
        root.getAsJsonObject("payload")?.getAsJsonArray("headers")?.forEach { h ->
            if (!h.isJsonObject) return@forEach
            val ho = h.asJsonObject
            val name = ho.get("name")?.asString?.lowercase() ?: return@forEach
            val value = ho.get("value")?.asString ?: ""
            when (name) {
                "subject" -> subject = value
                "from" -> from = value
                "date" -> date = value
            }
        }
        return GmailMessageDigest(id = id, snippet = snippet, from = from, subject = subject, date = date)
    }

    private fun extractPlainFromPayload(part: JsonObject?): String {
        if (part == null) return ""
        val mime = part.get("mimeType")?.asString
        val body = part.getAsJsonObject("body")
        val data = body?.get("data")?.asString
        if (mime == "text/plain" && data != null) {
            return decodeB64(data)
        }
        part.getAsJsonArray("parts")?.forEach { p ->
            if (p.isJsonObject) {
                val inner = extractPlainFromPayload(p.asJsonObject)
                if (inner.isNotBlank()) return inner
            }
        }
        return ""
    }

    private fun decodeB64(data: String): String = runCatching {
        val bytes = Base64.decode(data.replace('-', '+').replace('_', '/'), Base64.DEFAULT)
        String(bytes, StandardCharsets.UTF_8)
    }.getOrElse { "" }
}
