package com.dailycurator.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupStateStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val p = context.getSharedPreferences("backup_state", Context.MODE_PRIVATE)

    var driveFolderId: String?
        get() = p.getString(KEY_FOLDER, null)
        set(value) {
            p.edit().putString(KEY_FOLDER, value).apply()
        }

    fun recordUploadSuccess() {
        p.edit()
            .putLong(KEY_LAST_SUCCESS_MS, System.currentTimeMillis())
            .remove(KEY_LAST_ERROR)
            .apply()
    }

    fun recordUploadSkipped(reason: String) {
        p.edit().putString(KEY_LAST_ERROR, reason).apply()
    }

    fun recordUploadFailure(message: String) {
        p.edit().putString(KEY_LAST_ERROR, message).apply()
    }

    fun lastSuccessMillis(): Long = p.getLong(KEY_LAST_SUCCESS_MS, 0L)

    fun lastError(): String? = p.getString(KEY_LAST_ERROR, null)

    companion object {
        private const val KEY_FOLDER = "drive_folder_id"
        private const val KEY_LAST_SUCCESS_MS = "last_upload_success_ms"
        private const val KEY_LAST_ERROR = "last_upload_error"
    }
}
