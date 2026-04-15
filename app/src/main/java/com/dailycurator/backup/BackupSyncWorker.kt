package com.dailycurator.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val coordinator: AppBackupCoordinator,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        try {
            coordinator.runDriveBackup()
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "unknown")),
            )
        }

    companion object {
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}
