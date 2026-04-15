package com.dailycurator.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.work.ListenableWorker.Result as WorkResult
import com.dailycurator.MainActivity
import com.dailycurator.data.gmail.GmailTokenResult
import com.dailycurator.data.local.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBackupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packager: BackupPackager,
    private val drive: DriveBackupClient,
    private val tokenProvider: DriveAccessTokenProvider,
    private val state: BackupStateStore,
) {

    suspend fun runDriveBackup(): WorkResult = withContext(Dispatchers.IO) {
        val acc = GoogleSignIn.getLastSignedInAccount(context)
        val email = acc?.email?.trim().orEmpty()
        if (email.isEmpty()) {
            state.recordUploadSkipped("Not signed in with Google on this device.")
            return@withContext WorkResult.success()
        }
        when (val tr = tokenProvider.getAccessToken(email)) {
            is GmailTokenResult.Failure -> {
                state.recordUploadFailure(tr.message)
                WorkResult.retry()
            }
            is GmailTokenResult.NeedsUserInteraction -> {
                state.recordUploadSkipped("Complete Google sign-in in the app to enable Drive backup.")
                WorkResult.success()
            }
            is GmailTokenResult.Ok -> {
                val zip = try {
                    packager.createTempBackupZip()
                } catch (e: Exception) {
                    state.recordUploadFailure(e.message ?: "zip_failed")
                    return@withContext WorkResult.retry()
                }
                try {
                    val folderId = drive.findOrCreateBackupFolder(tr.accessToken, state.driveFolderId)
                    state.driveFolderId = folderId
                    drive.uploadZip(tr.accessToken, folderId, zip, packager.suggestedZipFileName())
                    drive.pruneOldBackups(tr.accessToken, folderId, DRIVE_RETENTION_COUNT)
                    state.recordUploadSuccess()
                    WorkResult.success()
                } catch (e: Exception) {
                    state.recordUploadFailure(e.message ?: "drive_upload_failed")
                    WorkResult.retry()
                } finally {
                    zip.delete()
                }
            }
        }
    }

    fun buildExportZipToCache(): File = packager.createTempBackupZip()

    fun suggestedBackupZipFileName(): String = packager.suggestedZipFileName()

    /**
     * Copies content from [uri] into a temp file, replaces local data, and restarts the process.
     * Must not be called while Room is actively in use from other threads; caller should pause UI.
     */
    fun restoreFromContentUri(uri: Uri) {
        val cache = File(context.cacheDir, "restore-${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cache).use { input.copyTo(it) }
        } ?: error("Could not read backup file")
        AppDatabase.destroyInstance()
        packager.restoreFromZipFile(cache)
        cache.delete()
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        Handler(Looper.getMainLooper()).post {
            context.startActivity(intent)
            Process.killProcess(Process.myPid())
        }
    }
}
