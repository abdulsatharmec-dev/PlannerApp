package com.dailycurator.backup

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.BufferedSink
import okio.buffer
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupClient @Inject constructor(
    private val http: OkHttpClient,
) {

    fun findOrCreateBackupFolder(accessToken: String, cachedFolderId: String?): String {
        if (!cachedFolderId.isNullOrBlank()) {
            if (folderExists(accessToken, cachedFolderId)) return cachedFolderId
        }
        val q = "mimeType='application/vnd.google-apps.folder' and name='${escapeQueryName(DRIVE_BACKUP_FOLDER_NAME)}' and 'root' in parents and trashed=false"
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("q", q)
            .addQueryParameter("fields", "files(id,name)")
            .addQueryParameter("pageSize", "10")
            .build()
        val body = httpGet(accessToken, url.toString())
        val files = JSONObject(body).optJSONArray("files") ?: JSONArray()
        if (files.length() > 0) {
            return files.getJSONObject(0).getString("id")
        }
        return createFolder(accessToken, DRIVE_BACKUP_FOLDER_NAME, parentId = "root")
    }

    private fun folderExists(accessToken: String, folderId: String): Boolean {
        val url = "https://www.googleapis.com/drive/v3/files/$folderId".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("fields", "id,trashed")
            .build()
        return try {
            val o = JSONObject(httpGet(accessToken, url.toString()))
            !o.optBoolean("trashed", false)
        } catch (_: Exception) {
            false
        }
    }

    private fun createFolder(accessToken: String, name: String, parentId: String): String {
        val json = JSONObject()
            .put("name", name)
            .put("mimeType", "application/vnd.google-apps.folder")
            .put("parents", JSONArray().put(parentId))
        val body = json.toString().toRequestBody(JSON_MEDIA)
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .header("Authorization", "Bearer $accessToken")
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Create folder failed ${resp.code}: $text")
            return JSONObject(text).getString("id")
        }
    }

    fun uploadZip(accessToken: String, folderId: String, zipFile: File, displayName: String) {
        val metadata = JSONObject()
            .put("name", displayName)
            .put("parents", JSONArray().put(folderId))
        val body = MultipartRelatedBody(metadata.toString(), zipFile)
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name")
            .header("Authorization", "Bearer $accessToken")
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Upload failed ${resp.code}: $text")
        }
    }

    fun pruneOldBackups(accessToken: String, folderId: String, keep: Int) {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("q", "'$folderId' in parents and trashed=false")
            .addQueryParameter("fields", "files(id,name,createdTime)")
            .addQueryParameter("orderBy", "createdTime desc")
            .addQueryParameter("pageSize", "100")
            .build()
            .toString()
        val body = httpGet(accessToken, url)
        val files = JSONObject(body).optJSONArray("files") ?: return
        if (files.length() <= keep) return
        for (i in keep until files.length()) {
            val id = files.getJSONObject(i).getString("id")
            deleteFile(accessToken, id)
        }
    }

    private fun deleteFile(accessToken: String, fileId: String) {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId")
            .header("Authorization", "Bearer $accessToken")
            .delete()
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 404) {
                val t = resp.body?.string().orEmpty()
                error("Delete failed ${resp.code}: $t")
            }
        }
    }

    private fun httpGet(accessToken: String, url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("GET failed ${resp.code}: $text")
            return text
        }
    }

    private fun escapeQueryName(name: String): String = name.replace("'", "\\'")

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

private class MultipartRelatedBody(
    private val metadataJson: String,
    private val file: File,
) : RequestBody() {
    private val boundary = "dayroute_${System.currentTimeMillis()}"
    private val contentType = "multipart/related; boundary=$boundary".toMediaType()

    override fun contentType() = contentType

    override fun writeTo(sink: BufferedSink) {
        val b = boundary
        sink.writeUtf8("--$b\r\n")
        sink.writeUtf8("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        sink.writeUtf8(metadataJson)
        sink.writeUtf8("\r\n--$b\r\n")
        sink.writeUtf8("Content-Type: application/zip\r\n\r\n")
        file.source().use { sink.writeAll(it) }
        sink.writeUtf8("\r\n--$b--\r\n")
    }
}
