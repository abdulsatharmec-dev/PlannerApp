package com.dailycurator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dailycurator.data.local.dao.AgentMemoryDao
import com.dailycurator.data.local.dao.CachedInsightDao
import com.dailycurator.data.local.dao.ChatMessageDao
import com.dailycurator.data.local.dao.GoalDao
import com.dailycurator.data.local.dao.HabitDao
import com.dailycurator.data.local.dao.HabitLogDao
import com.dailycurator.data.local.dao.JournalDao
import com.dailycurator.data.local.dao.PomodoroDao
import com.dailycurator.data.local.dao.TaskDao
import com.dailycurator.data.local.entity.AgentMemoryEntity
import com.dailycurator.data.local.entity.CachedInsightEntity
import com.dailycurator.data.local.entity.ChatMessageEntity
import com.dailycurator.data.local.entity.GoalEntity
import com.dailycurator.data.local.entity.HabitEntity
import com.dailycurator.data.local.entity.HabitLogEntity
import com.dailycurator.data.local.entity.JournalEntryEntity
import com.dailycurator.data.local.entity.PomodoroSessionEntity
import com.dailycurator.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        TaskEntity::class, HabitEntity::class, GoalEntity::class, ChatMessageEntity::class,
        CachedInsightEntity::class, JournalEntryEntity::class, HabitLogEntity::class,
        PomodoroSessionEntity::class, AgentMemoryEntity::class,
    ],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun cachedInsightDao(): CachedInsightDao
    abstract fun journalDao(): JournalDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun agentMemoryDao(): AgentMemoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `content` TEXT NOT NULL,
                        `isUser` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_insights` (
                        `type` TEXT NOT NULL PRIMARY KEY,
                        `dayKey` TEXT NOT NULL,
                        `generatedAtEpochMillis` INTEGER NOT NULL,
                        `insightText` TEXT NOT NULL,
                        `boldPart` TEXT NOT NULL,
                        `recoveryPlan` TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `journal_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agent_memory` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `content` TEXT NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        `isManual` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN totalTokens INTEGER")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isMustDo INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN displayNumber INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isTopFive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE tasks SET isTopFive = 1 WHERE rank >= 1 AND rank <= 5")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN progressPercent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN iconEmoji TEXT")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN goalId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tasks_goalId` ON `tasks` (`goalId`)",
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE journal_entries ADD COLUMN includeInAgentChat INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE journal_entries ADD COLUMN includeInAssistantInsight INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE journal_entries ADD COLUMN includeInWeeklyGoalsInsight INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_insights ADD COLUMN spiritualSource TEXT")
                db.execSQL("ALTER TABLE cached_insights ADD COLUMN spiritualArabic TEXT")
                db.execSQL("ALTER TABLE cached_insights ADD COLUMN spiritualEnglish TEXT")
                db.execSQL("ALTER TABLE cached_insights ADD COLUMN summarySegmentsJson TEXT")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE journal_entries ADD COLUMN isEvergreen INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatSeriesId TEXT")
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN repeatOption TEXT NOT NULL DEFAULT 'NONE'",
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN customRepeatIntervalDays INTEGER NOT NULL DEFAULT 3",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tasks_repeatSeriesId` ON `tasks` (`repeatSeriesId`)",
                )
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatUntilDate TEXT")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isCantComplete INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN voiceRelativePath TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN seriesId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN longestStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE habits SET seriesId = CAST(id AS TEXT) WHERE seriesId = ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitSeriesId` TEXT NOT NULL,
                        `dayKey` TEXT NOT NULL,
                        `note` TEXT,
                        `loggedAtMillis` INTEGER NOT NULL,
                        `valueCompleted` REAL NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitSeriesId_dayKey`
                    ON `habit_logs` (`habitSeriesId`, `dayKey`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pomodoro_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` INTEGER NOT NULL,
                        `habitSeriesId` TEXT,
                        `title` TEXT NOT NULL,
                        `startedAtMillis` INTEGER NOT NULL,
                        `endedAtMillis` INTEGER,
                        `plannedDurationSeconds` INTEGER NOT NULL,
                        `actualFocusedSeconds` INTEGER NOT NULL DEFAULT 0,
                        `completed` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_pomodoro_sessions_entityType_entityId`
                    ON `pomodoro_sessions` (`entityType`, `entityId`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_pomodoro_sessions_endedAtMillis`
                    ON `pomodoro_sessions` (`endedAtMillis`)
                    """.trimIndent(),
                )
            }
        }

        /** Close and drop the singleton so a new DB file can replace [Context.getDatabasePath]. */
        fun destroyInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "daily_curator.db")
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch { seedDatabase(database) }
                            }
                        }
                    })
                    .build().also { INSTANCE = it }
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val today = LocalDate.now().format(DATE_FMT)
            val weekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                .format(DATE_FMT)

            // Seed tasks
            db.taskDao().apply {
                insert(TaskEntity(rank = 1, title = "Finalize Q4 Budget Sheets",
                    startTime = "09:00", endTime = "10:30", dueInfo = "Due by 4:00 PM",
                    urgency = "GREEN", date = today, isProtected = false, isTopFive = true))
                insert(TaskEntity(rank = 2, title = "Contract Review: Acme Corp",
                    startTime = "11:00", endTime = "12:00", statusNote = "Legal approval pending",
                    urgency = "GREEN", date = today, isTopFive = true))
                insert(TaskEntity(rank = 3, title = "Hiring: Senior UI Designer",
                    startTime = "13:30", endTime = "14:30", statusNote = "Review 5 portfolios",
                    urgency = "GREEN", date = today, isTopFive = true))
                insert(TaskEntity(rank = 4, title = "Presentation Prep: Board Meeting",
                    startTime = "15:00", endTime = "16:30", statusNote = "Slide 12-24 remaining",
                    urgency = "RED", date = today, isTopFive = true))
                insert(TaskEntity(rank = 5, title = "Bi-Weekly Sync: Marketing",
                    startTime = "16:30", endTime = "17:00", statusNote = "External Agency included",
                    urgency = "NEUTRAL", date = today, isTopFive = true))
            }

            // Seed habits
            db.habitDao().apply {
                insert(HabitEntity(seriesId = "seed-hydration", longestStreak = 12,
                    name = "Hydration Protocol", category = "PHYSICAL",
                    habitType = "BUILDING", iconEmoji = "💧",
                    currentValue = 2.2f, targetValue = 3.0f, unit = "L",
                    streakDays = 12, date = today))
                insert(HabitEntity(seriesId = "seed-focus", longestStreak = 5,
                    name = "Deep Focus Session", category = "MENTAL",
                    habitType = "BUILDING", iconEmoji = "🧘",
                    currentValue = 3f, targetValue = 5f, unit = "sessions",
                    streakDays = 5, date = today))
                insert(HabitEntity(seriesId = "seed-reading", longestStreak = 28,
                    name = "Sacred Reading", category = "SPIRITUAL",
                    habitType = "BUILDING", iconEmoji = "📖",
                    currentValue = 20f, targetValue = 20f, unit = "mins",
                    streakDays = 28, date = today))
                insert(HabitEntity(seriesId = "seed-digital", longestStreak = 3,
                    name = "Digital Distraction", category = "MENTAL",
                    habitType = "ELIMINATING", iconEmoji = "📱",
                    currentValue = 15f, targetValue = 30f, unit = "min limit",
                    streakDays = 3, date = today))
            }

            // Seed goals
            db.goalDao().apply {
                insert(GoalEntity(title = "Finalize Q3 Pipeline Audit", isCompleted = true, weekStart = weekStart))
                insert(GoalEntity(title = "Client Showcase Asset Selection", isCompleted = true, weekStart = weekStart))
                insert(GoalEntity(title = "Weekly Team Performance Sync", isCompleted = true, weekStart = weekStart))
                insert(GoalEntity(title = "Budget Forecast v2.0", isCompleted = false, weekStart = weekStart))
                insert(GoalEntity(title = "Internal Architecture Refactor", isCompleted = false, weekStart = weekStart))
            }
        }
    }
}
