package com.dailycurator.backup

import android.content.Context
import android.content.pm.PackageManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.dailycurator.data.local.AppDatabase
import com.dailycurator.ui.screens.journal.JournalVoiceFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupPackager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {

    private val backupCacheDir: File
        get() = File(context.cacheDir, "backups").apply { mkdirs() }

    fun suggestedZipFileName(): String {
        val fmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "$ZIP_NAME_PREFIX${fmt.format(Date())}.zip"
    }

    /**
     * Writes a ZIP to [targetFile] (parent dirs must exist). Runs a WAL checkpoint first.
     */
    fun writeBackupZip(targetFile: File) {
        targetFile.parentFile?.mkdirs()
        val dbPath = context.getDatabasePath(ROOM_DB_FILE_NAME)
        // PRAGMA wal_checkpoint returns rows — must use query(), not execSQL(), on SupportSQLiteDatabase.
        database.openHelper.writableDatabase.query(
            SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"),
        ).use { it.moveToFirst() }
        val manifest = BackupManifest(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            appVersionName = readVersionName(),
            createdAtUtcMillis = System.currentTimeMillis(),
            packageName = context.packageName,
        )
        val prefsJson = exportCuratorPrefsToJson(context)
        ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
            zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zos.write(manifest.toJson().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry(PREFS_ENTRY))
            zos.write(prefsJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry(DB_ZIP_ENTRY))
            FileInputStream(dbPath).use { it.copyTo(zos) }
            zos.closeEntry()
            val voiceDir = JournalVoiceFiles.voiceDir(context)
            if (voiceDir.isDirectory) {
                voiceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val rel = file.relativeTo(context.filesDir).path.replace(File.separatorChar, '/')
                    val entryName = "attachments/$rel"
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    fun createTempBackupZip(): File {
        trimOldBackupZipFiles()
        val f = File(backupCacheDir, "temp-${System.currentTimeMillis()}.zip")
        writeBackupZip(f)
        return f
    }

    private fun trimOldBackupZipFiles(keepMostRecent: Int = 8) {
        val zips = backupCacheDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".zip", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        zips.drop(keepMostRecent).forEach { it.delete() }
    }

    private fun readVersionName(): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (_: PackageManager.NameNotFoundException) {
            "?"
        }

    /**
     * Validates manifest and replaces DB, prefs, and journal_voice files. Caller must have closed Room.
     */
    fun restoreFromZipFile(zipFile: File) {
        val unpack = File(context.cacheDir, "restore_unpack_${System.currentTimeMillis()}").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val relative = entry.name.trimStart('/').replace("\\", "/")
                        require(!relative.contains("..")) { "Illegal path in backup zip" }
                        val out = File(unpack, relative.replace("/", File.separator))
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            val manifestFile = File(unpack, MANIFEST_ENTRY)
            require(manifestFile.exists()) { "Missing manifest" }
            val manifest = BackupManifest.parse(manifestFile.readText(Charsets.UTF_8))
            require(manifest.schemaVersion == BACKUP_SCHEMA_VERSION) {
                "Unsupported backup version ${manifest.schemaVersion}"
            }
            val prefsFile = File(unpack, PREFS_ENTRY)
            require(prefsFile.exists()) { "Missing prefs" }
            restoreCuratorPrefsFromJson(context, prefsFile.readText(Charsets.UTF_8))
            val dbExtracted = File(unpack, DB_ZIP_ENTRY)
            require(dbExtracted.exists()) { "Missing database in backup" }
            val dbTarget = context.getDatabasePath(ROOM_DB_FILE_NAME)
            dbTarget.parentFile?.mkdirs()
            dbExtracted.copyTo(dbTarget, overwrite = true)
            val attachmentsRoot = File(unpack, "attachments/journal_voice")
            val targetVoice = JournalVoiceFiles.voiceDir(context)
            if (attachmentsRoot.isDirectory) {
                targetVoice.deleteRecursively()
                targetVoice.mkdirs()
                attachmentsRoot.copyRecursively(targetVoice, overwrite = true)
            } else {
                targetVoice.deleteRecursively()
                targetVoice.mkdirs()
            }
        } finally {
            unpack.deleteRecursively()
        }
    }
}
