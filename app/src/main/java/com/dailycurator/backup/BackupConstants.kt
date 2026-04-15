package com.dailycurator.backup

/** Unique work name for one-tap “Back up to Drive now” (observed in Settings for status). */
const val MANUAL_DRIVE_BACKUP_UNIQUE_WORK = "dayroute_drive_backup_manual"

/** Must match [com.dailycurator.data.local.AppPreferences] internal name. */
internal const val CURATOR_PREFS_NAME = "curator_prefs"

internal const val ROOM_DB_FILE_NAME = "daily_curator.db"

internal const val BACKUP_SCHEMA_VERSION = 1

internal const val DRIVE_BACKUP_FOLDER_NAME = "DayRoute backups"

internal const val ZIP_NAME_PREFIX = "dayroute-backup-"

internal const val MANIFEST_ENTRY = "manifest.json"

internal const val PREFS_ENTRY = "prefs.json"

internal const val DB_ZIP_ENTRY = "data/$ROOM_DB_FILE_NAME"

internal const val JOURNAL_VOICE_ZIP_PREFIX = "attachments/journal_voice/"

internal const val DRIVE_RETENTION_COUNT = 20
