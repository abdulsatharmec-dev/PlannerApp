package com.dailycurator.backup

import org.json.JSONObject

internal data class BackupManifest(
    val schemaVersion: Int,
    val appVersionName: String,
    val createdAtUtcMillis: Long,
    val packageName: String,
) {
    fun toJson(): String =
        JSONObject()
            .put("schemaVersion", schemaVersion)
            .put("appVersionName", appVersionName)
            .put("createdAtUtcMillis", createdAtUtcMillis)
            .put("packageName", packageName)
            .toString()

    companion object {
        fun parse(json: String): BackupManifest {
            val o = JSONObject(json)
            return BackupManifest(
                schemaVersion = o.getInt("schemaVersion"),
                appVersionName = o.optString("appVersionName", ""),
                createdAtUtcMillis = o.getLong("createdAtUtcMillis"),
                packageName = o.optString("packageName", ""),
            )
        }
    }
}
