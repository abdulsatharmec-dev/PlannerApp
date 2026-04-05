package com.dailycurator.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dailycurator.data.ai.AiPromptDefaults
import com.dailycurator.data.gmail.GmailLinkedAccountPref
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
private const val KEY_GMAIL_ACCOUNTS_JSON = "gmail_accounts_json"
private const val KEY_AGENT_GMAIL_READ = "agent_gmail_read_enabled"
private const val KEY_AGENT_GMAIL_SEND = "agent_gmail_send_enabled"
private const val KEY_GMAIL_SUMMARY_PROMPT = "gmail_mailbox_summary_prompt"
private const val KEY_HOME_GMAIL_SUMMARY = "home_gmail_summary_enabled"
private const val KEY_AGENT_MEMORY_ENABLED = "agent_memory_enabled"
private const val KEY_MEMORY_EXTRACTION_PROMPT = "memory_extraction_prompt"
private const val KEY_MAILBOX_SUMMARY_RANGE_DAYS = "mailbox_summary_range_days"

/** Minutes from midnight; inclusive schedule / “day” window on the home screen. */
data class DayWindowMinutes(val startMinute: Int, val endMinute: Int)

private const val DEFAULT_DAY_START_MIN = 4 * 60   // 4:00
private const val DEFAULT_DAY_END_MIN = 22 * 60    // 22:00

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gmailAccountListType = object : TypeToken<ArrayList<GmailLinkedAccountPref>>() {}.type

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

    val gmailAccountsFlow: Flow<List<GmailLinkedAccountPref>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_GMAIL_ACCOUNTS_JSON) trySend(getGmailLinkedAccounts())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getGmailLinkedAccounts())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val agentGmailReadEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_AGENT_GMAIL_READ) trySend(isAgentGmailReadEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isAgentGmailReadEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val agentGmailSendEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_AGENT_GMAIL_SEND) trySend(isAgentGmailSendEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isAgentGmailSendEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homeGmailSummaryEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_GMAIL_SUMMARY) trySend(isHomeGmailSummaryEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isHomeGmailSummaryEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val agentMemoryEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_AGENT_MEMORY_ENABLED) trySend(isAgentMemoryEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isAgentMemoryEnabled())
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

    fun getGmailLinkedAccounts(): List<GmailLinkedAccountPref> {
        val raw = prefs.getString(KEY_GMAIL_ACCOUNTS_JSON, null) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val parsed = runCatching {
            gson.fromJson<MutableList<GmailLinkedAccountPref>>(raw, gmailAccountListType)
        }.getOrNull()
        return parsed ?: emptyList()
    }

    fun setGmailLinkedAccounts(accounts: List<GmailLinkedAccountPref>) {
        prefs.edit().putString(KEY_GMAIL_ACCOUNTS_JSON, gson.toJson(accounts)).apply()
    }

    fun upsertGmailLinkedAccount(account: GmailLinkedAccountPref) {
        val cur = getGmailLinkedAccounts().toMutableList()
        val idx = cur.indexOfFirst { it.email.equals(account.email, ignoreCase = true) }
        if (idx >= 0) cur[idx] = account else cur.add(account)
        setGmailLinkedAccounts(cur)
    }

    fun removeGmailLinkedAccount(email: String) {
        setGmailLinkedAccounts(
            getGmailLinkedAccounts().filterNot { it.email.equals(email, ignoreCase = true) },
        )
    }

    fun setGmailSummaryVisible(email: String, visible: Boolean) {
        val cur = getGmailLinkedAccounts().map {
            if (it.email.equals(email, ignoreCase = true)) it.copy(showInSummary = visible) else it
        }
        setGmailLinkedAccounts(cur)
    }

    fun isAgentGmailReadEnabled(): Boolean =
        prefs.getBoolean(KEY_AGENT_GMAIL_READ, false)

    fun setAgentGmailReadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AGENT_GMAIL_READ, enabled).apply()
    }

    fun isAgentGmailSendEnabled(): Boolean =
        prefs.getBoolean(KEY_AGENT_GMAIL_SEND, false)

    fun setAgentGmailSendEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AGENT_GMAIL_SEND, enabled).apply()
    }

    fun getGmailMailboxSummaryPrompt(): String {
        val raw = prefs.getString(KEY_GMAIL_SUMMARY_PROMPT, null)?.trim().orEmpty()
        return raw.ifEmpty { AiPromptDefaults.GMAIL_MAILBOX_SUMMARY }
    }

    fun setGmailMailboxSummaryPrompt(prompt: String) {
        prefs.edit().putString(KEY_GMAIL_SUMMARY_PROMPT, prompt).apply()
    }

    fun isHomeGmailSummaryEnabled(): Boolean =
        prefs.getBoolean(KEY_HOME_GMAIL_SUMMARY, false)

    fun setHomeGmailSummaryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_GMAIL_SUMMARY, enabled).apply()
    }

    fun isAgentMemoryEnabled(): Boolean =
        prefs.getBoolean(KEY_AGENT_MEMORY_ENABLED, true)

    fun setAgentMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AGENT_MEMORY_ENABLED, enabled).apply()
    }

    fun getMemoryExtractionPrompt(): String {
        val raw = prefs.getString(KEY_MEMORY_EXTRACTION_PROMPT, null)?.trim().orEmpty()
        return raw.ifEmpty { AiPromptDefaults.MEMORY_EXTRACTION }
    }

    fun setMemoryExtractionPrompt(prompt: String) {
        prefs.edit().putString(KEY_MEMORY_EXTRACTION_PROMPT, prompt).apply()
    }

    fun getMailboxSummaryRangeDays(): Int =
        prefs.getInt(KEY_MAILBOX_SUMMARY_RANGE_DAYS, 7).coerceIn(1, 90)

    fun setMailboxSummaryRangeDays(days: Int) {
        prefs.edit().putInt(KEY_MAILBOX_SUMMARY_RANGE_DAYS, days.coerceIn(1, 90)).apply()
    }
}
