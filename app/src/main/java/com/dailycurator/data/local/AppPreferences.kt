package com.dailycurator.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dailycurator.data.ai.AiPromptDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "curator_prefs"
private const val KEY_DARK = "dark_theme"
private const val KEY_CEREBRAS = "cerebras_key"
private const val KEY_CEREBRAS_MODEL = "cerebras_model_id"
private const val KEY_ASSISTANT_INSIGHT_ENABLED = "assistant_insight_enabled"
private const val KEY_WEEKLY_GOALS_INSIGHT_ENABLED = "weekly_goals_insight_enabled"
private const val KEY_ASSISTANT_INSIGHT_PROMPT = "assistant_insight_prompt"
private const val KEY_WEEKLY_GOALS_INSIGHT_PROMPT = "weekly_goals_insight_prompt"
private const val KEY_DAY_WINDOW_START_MIN = "day_window_start_min"
private const val KEY_DAY_WINDOW_END_MIN = "day_window_end_min"
private const val KEY_JOURNAL_SHARE_WITH_CHAT = "journal_share_with_chat"
private const val KEY_JOURNAL_IN_ASSISTANT_INSIGHT = "journal_in_assistant_insight"
private const val KEY_JOURNAL_IN_WEEKLY_GOALS_INSIGHT = "journal_in_weekly_goals_insight"

/** Minutes from midnight; inclusive schedule / “day” window on the home screen. */
data class DayWindowMinutes(val startMinute: Int, val endMinute: Int)

private const val DEFAULT_DAY_START_MIN = 4 * 60   // 4:00
private const val DEFAULT_DAY_END_MIN = 22 * 60    // 22:00

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Emits the current dark-theme value and re-emits whenever it changes. */
    val darkThemeFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DARK) trySend(prefs.getBoolean(KEY_DARK, false))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_DARK, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val assistantInsightEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ASSISTANT_INSIGHT_ENABLED) trySend(isAssistantInsightEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isAssistantInsightEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val weeklyGoalsInsightEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_WEEKLY_GOALS_INSIGHT_ENABLED) trySend(isWeeklyGoalsInsightEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isWeeklyGoalsInsightEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val cerebrasKeyPresentFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CEREBRAS) trySend(getCerebrasKey().isNotBlank())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getCerebrasKey().isNotBlank())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val dayWindowFlow: Flow<DayWindowMinutes> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DAY_WINDOW_START_MIN || key == KEY_DAY_WINDOW_END_MIN) {
                trySend(getDayWindow())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getDayWindow())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val journalShareWithChatFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_JOURNAL_SHARE_WITH_CHAT) trySend(isJournalSharedWithChat())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isJournalSharedWithChat())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val journalInAssistantInsightFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_JOURNAL_IN_ASSISTANT_INSIGHT) trySend(isJournalInAssistantInsight())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isJournalInAssistantInsight())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val journalInWeeklyGoalsInsightFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_JOURNAL_IN_WEEKLY_GOALS_INSIGHT) trySend(isJournalInWeeklyGoalsInsight())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isJournalInWeeklyGoalsInsight())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, enabled).apply()
    }

    fun setCerebrasKey(key: String) {
        prefs.edit().putString(KEY_CEREBRAS, key).apply()
    }

    fun getCerebrasKey(): String = prefs.getString(KEY_CEREBRAS, "") ?: ""

    fun setCerebrasModelId(modelId: String) {
        prefs.edit().putString(KEY_CEREBRAS_MODEL, modelId.trim()).apply()
    }

    fun getCerebrasModelId(): String {
        val raw = prefs.getString(KEY_CEREBRAS_MODEL, "")?.trim().orEmpty()
        return raw.ifEmpty { com.dailycurator.data.local.DEFAULT_CEREBRAS_MODEL_ID }
    }

    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK, false)

    fun isAssistantInsightEnabled(): Boolean =
        prefs.getBoolean(KEY_ASSISTANT_INSIGHT_ENABLED, true)

    fun setAssistantInsightEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASSISTANT_INSIGHT_ENABLED, enabled).apply()
    }

    fun isWeeklyGoalsInsightEnabled(): Boolean =
        prefs.getBoolean(KEY_WEEKLY_GOALS_INSIGHT_ENABLED, true)

    fun setWeeklyGoalsInsightEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY_GOALS_INSIGHT_ENABLED, enabled).apply()
    }

    fun getAssistantInsightPrompt(): String {
        val raw = prefs.getString(KEY_ASSISTANT_INSIGHT_PROMPT, null)?.trim().orEmpty()
        return raw.ifEmpty { AiPromptDefaults.ASSISTANT_INSIGHT }
    }

    fun setAssistantInsightPrompt(prompt: String) {
        prefs.edit().putString(KEY_ASSISTANT_INSIGHT_PROMPT, prompt).apply()
    }

    fun getWeeklyGoalsInsightPrompt(): String {
        val raw = prefs.getString(KEY_WEEKLY_GOALS_INSIGHT_PROMPT, null)?.trim().orEmpty()
        return raw.ifEmpty { AiPromptDefaults.WEEKLY_GOALS_INSIGHT }
    }

    fun setWeeklyGoalsInsightPrompt(prompt: String) {
        prefs.edit().putString(KEY_WEEKLY_GOALS_INSIGHT_PROMPT, prompt).apply()
    }

    fun getDayWindowStartMinutes(): Int =
        prefs.getInt(KEY_DAY_WINDOW_START_MIN, DEFAULT_DAY_START_MIN).coerceIn(0, 23 * 60 + 59)

    fun getDayWindowEndMinutes(): Int =
        prefs.getInt(KEY_DAY_WINDOW_END_MIN, DEFAULT_DAY_END_MIN).coerceIn(1, 24 * 60 - 1)

    fun getDayWindow(): DayWindowMinutes {
        var s = getDayWindowStartMinutes()
        var e = getDayWindowEndMinutes()
        if (e <= s) e = (s + 60).coerceAtMost(24 * 60 - 1)
        return DayWindowMinutes(s, e)
    }

    /**
     * Persist start of the “active day” window. If this is no longer strictly before the end,
     * the end is pushed forward to keep a valid range.
     */
    fun setDayWindowStartMinutes(minuteOfDay: Int) {
        val s = minuteOfDay.coerceIn(0, 23 * 60 + 59)
        var e = getDayWindowEndMinutes()
        if (e <= s) e = (s + 60).coerceAtMost(24 * 60 - 1)
        prefs.edit()
            .putInt(KEY_DAY_WINDOW_START_MIN, s)
            .putInt(KEY_DAY_WINDOW_END_MIN, e)
            .apply()
    }

    fun setDayWindowEndMinutes(minuteOfDay: Int) {
        val e = minuteOfDay.coerceIn(1, 24 * 60 - 1)
        var s = getDayWindowStartMinutes()
        if (e <= s) s = (e - 60).coerceAtLeast(0)
        prefs.edit()
            .putInt(KEY_DAY_WINDOW_START_MIN, s)
            .putInt(KEY_DAY_WINDOW_END_MIN, e)
            .apply()
    }

    fun isJournalSharedWithChat(): Boolean =
        prefs.getBoolean(KEY_JOURNAL_SHARE_WITH_CHAT, true)

    fun setJournalSharedWithChat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_JOURNAL_SHARE_WITH_CHAT, enabled).apply()
    }

    fun isJournalInAssistantInsight(): Boolean =
        prefs.getBoolean(KEY_JOURNAL_IN_ASSISTANT_INSIGHT, true)

    fun setJournalInAssistantInsight(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_JOURNAL_IN_ASSISTANT_INSIGHT, enabled).apply()
    }

    fun isJournalInWeeklyGoalsInsight(): Boolean =
        prefs.getBoolean(KEY_JOURNAL_IN_WEEKLY_GOALS_INSIGHT, true)

    fun setJournalInWeeklyGoalsInsight(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_JOURNAL_IN_WEEKLY_GOALS_INSIGHT, enabled).apply()
    }
}
