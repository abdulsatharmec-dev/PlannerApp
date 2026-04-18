package com.dailycurator.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dailycurator.data.ai.AiPromptDefaults
import com.dailycurator.data.gmail.GmailLinkedAccountPref
import com.dailycurator.data.media.MorningPlaylistPref
import com.dailycurator.data.media.MorningVideoBucket
import com.dailycurator.notifications.AlertToneKind
import com.dailycurator.notifications.NotificationTonePack
import com.dailycurator.ui.theme.AppBackgroundOption
import com.dailycurator.ui.theme.AppThemePalette
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "curator_prefs"
private const val KEY_DARK = "dark_theme"
private const val KEY_THEME_PALETTE = "theme_palette_id"
private const val KEY_APP_BACKGROUND = "app_background_id"
private const val KEY_CUSTOM_WALLPAPER_URI = "custom_wallpaper_uri"
private const val KEY_CHAT_FONT_SIZE_CATEGORY = "chat_font_size_category"
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
private const val KEY_JOURNAL_CONTEXT_WINDOW_DAYS = "journal_context_window_days"
private const val KEY_GMAIL_ACCOUNTS_JSON = "gmail_accounts_json"
private const val KEY_AGENT_GMAIL_READ = "agent_gmail_read_enabled"
private const val KEY_AGENT_GMAIL_SEND = "agent_gmail_send_enabled"
private const val KEY_GMAIL_SUMMARY_PROMPT = "gmail_mailbox_summary_prompt"
private const val KEY_HOME_GMAIL_SUMMARY = "home_gmail_summary_enabled"
private const val KEY_AGENT_MEMORY_ENABLED = "agent_memory_enabled"
private const val KEY_MEMORY_EXTRACTION_PROMPT = "memory_extraction_prompt"
private const val KEY_MAILBOX_SUMMARY_RANGE_DAYS = "mailbox_summary_range_days"
private const val KEY_PHONE_USAGE_INSIGHT_PROMPT = "phone_usage_insight_prompt"
private const val KEY_PHONE_USAGE_IN_CHAT_AGENT = "phone_usage_in_chat_agent"
private const val KEY_PHONE_USAGE_IN_ASSISTANT_INSIGHT = "phone_usage_in_assistant_insight"
private const val KEY_PHONE_USAGE_IN_WEEKLY_GOALS_INSIGHT = "phone_usage_in_weekly_goals_insight"
private const val KEY_PHONE_USAGE_AI_CONTEXT_DAYS = "phone_usage_ai_context_days"
private const val KEY_PHONE_USAGE_WEEKLY_INSIGHT_DAYS = "phone_usage_weekly_insight_days"
private const val KEY_PHONE_USAGE_SCREEN_RANGE_DAYS = "phone_usage_screen_range_days"
private const val KEY_LLM_PROFILES_JSON = "llm_api_key_profiles_json"
private const val KEY_MORNING_YOUTUBE_LINES = "morning_motivation_youtube_lines"
private const val KEY_MORNING_LOCAL_VIDEO_URIS_JSON = "morning_motivation_local_video_uris_json"
private const val KEY_MORNING_PLAYLIST_ENTRIES_JSON = "morning_playlist_entries_json"
private const val KEY_MORNING_PLAYLIST_DEFAULTS_APPLIED = "morning_playlist_defaults_applied_v1"
private const val KEY_MORNING_MOTIVATION_AUTOPLAY = "morning_motivation_autoplay"
private const val KEY_HOME_DAILY_PDF_URI = "home_daily_pdf_uri"
private const val KEY_HOME_DAILY_PDF_LAST_PAGE = "home_daily_pdf_last_page"
private const val KEY_HOME_PDF_VIEW_MODE = "home_pdf_view_mode"
private const val KEY_HOME_PDF_THEME_DARK = "home_pdf_theme_dark"
private const val KEY_HOME_PDF_ZOOM_SCALE = "home_pdf_zoom_scale"
private const val KEY_HOME_LAYOUT_SECTION_ORDER_JSON = "home_layout_section_order_json"
private const val KEY_SHOW_MUST_DO_TASKS = "tasks_show_must_do_v1"
private const val KEY_SHOW_WONT_DO_TASKS = "tasks_show_wont_do_v1"
private const val KEY_TASK_TAG_COLORS_JSON = "task_tag_colors_json_v1"
private const val KEY_HOME_MORNING_VIDEO_TAB = "home_morning_video_tab"
private const val KEY_MORNING_SPIRITUAL_YOUTUBE_LINES = "morning_spiritual_youtube_lines"
private const val KEY_MORNING_SPIRITUAL_LOCAL_VIDEO_URIS_JSON = "morning_spiritual_local_video_uris_json"
private const val KEY_MORNING_SPIRITUAL_PLAYLIST_ENTRIES_JSON = "morning_spiritual_playlist_entries_json"
private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
private const val KEY_APP_LOCK_PIN_SALT = "app_lock_pin_salt"
private const val KEY_APP_LOCK_PIN_HASH = "app_lock_pin_hash"
private const val KEY_APP_LOCK_ALLOW_BIOMETRIC = "app_lock_allow_biometric"
private const val KEY_REMINDER_TONE_KIND = "reminder_tone_kind_v1"
private const val KEY_REMINDER_TONE_CUSTOM_URI = "reminder_tone_custom_uri_v1"
private const val KEY_POMO_TONE_KIND = "pomo_tone_kind_v1"
private const val KEY_POMO_TONE_CUSTOM_URI = "pomo_tone_custom_uri_v1"
private const val KEY_NOTIFICATION_TONE_PACK_ID = "notification_tone_pack_id_v1"
private const val KEY_LEGACY_ALERT_SOUND_PRESET = "alert_sound_preset_v1"

/** Minutes from midnight; inclusive schedule / “day” window on the home screen. */
data class DayWindowMinutes(val startMinute: Int, val endMinute: Int)

private const val DEFAULT_DAY_START_MIN = 4 * 60   // 4:00
private const val DEFAULT_DAY_END_MIN = 22 * 60    // 22:00

private val defaultMorningPlaylists: List<MorningPlaylistPref> = listOf(
    MorningPlaylistPref(
        playlistId = "PLQ02IYL5pmhFYDrmxNHAlwgcHOR4h1bPa",
        sourceWatchUrl = "https://www.youtube.com/watch?v=1mub-ru8BTo&list=PLQ02IYL5pmhFYDrmxNHAlwgcHOR4h1bPa",
    ),
    MorningPlaylistPref(
        playlistId = "PLQ02IYL5pmhH0KWBrGxxR-lCCwlTFIVbm",
        sourceWatchUrl = "https://www.youtube.com/watch?v=MfDb8isHUtk&list=PLQ02IYL5pmhH0KWBrGxxR-lCCwlTFIVbm",
    ),
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gmailAccountListType = object : TypeToken<ArrayList<GmailLinkedAccountPref>>() {}.type
    private val llmProfileListType = object : TypeToken<ArrayList<LlmApiKeyProfile>>() {}.type
    private val stringListType = object : TypeToken<ArrayList<String>>() {}.type
    private val morningPlaylistListType = object : TypeToken<ArrayList<MorningPlaylistPref>>() {}.type
    private val stringIntMapType = object : TypeToken<HashMap<String, Int>>() {}.type

    /** Emits the current dark-theme value and re-emits whenever it changes. */
    val darkThemeFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DARK) trySend(prefs.getBoolean(KEY_DARK, false))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_DARK, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** Selected accent palette id ([AppThemePalette.storageId]). */
    val themePaletteIdFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_PALETTE) trySend(getThemePaletteId())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getThemePaletteId())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** Decorative background id ([AppBackgroundOption.storageId]). */
    val appBackgroundIdFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_APP_BACKGROUND) trySend(getAppBackgroundId())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAppBackgroundId())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val customWallpaperUriFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CUSTOM_WALLPAPER_URI) trySend(getCustomWallpaperUri())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getCustomWallpaperUri())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val chatFontSizeCategoryFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CHAT_FONT_SIZE_CATEGORY) trySend(getChatFontSizeCategoryId())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getChatFontSizeCategoryId())
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

    /** True when at least one LLM profile is enabled or a legacy Cerebras key is set. */
    val llmConfiguredFlow: Flow<Boolean> = callbackFlow {
        val watchKeys = setOf(KEY_CEREBRAS, KEY_CEREBRAS_MODEL, KEY_LLM_PROFILES_JSON)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in watchKeys) trySend(isLlmConfigured())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isLlmConfigured())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** @deprecated Prefer [llmConfiguredFlow]; same stream. */
    val cerebrasKeyPresentFlow: Flow<Boolean> = llmConfiguredFlow

    val llmProfilesFlow: Flow<List<LlmApiKeyProfile>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LLM_PROFILES_JSON) trySend(getLlmProfiles())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getLlmProfiles())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** Raw multiline text: one YouTube URL or 11-char id per line (shown/edited in Settings). */
    val morningYoutubeLinesBlockFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING_YOUTUBE_LINES) trySend(getMorningYoutubeLinesBlock())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getMorningYoutubeLinesBlock())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val morningLocalVideoUrisFlow: Flow<List<String>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING_LOCAL_VIDEO_URIS_JSON) trySend(getMorningLocalVideoUris())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getMorningLocalVideoUris())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val morningPlaylistEntriesFlow: Flow<List<MorningPlaylistPref>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING_PLAYLIST_ENTRIES_JSON ||
                key == KEY_MORNING_PLAYLIST_DEFAULTS_APPLIED
            ) {
                trySend(getMorningPlaylistEntries())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getMorningPlaylistEntries())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val morningSpiritualYoutubeLinesBlockFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING_SPIRITUAL_YOUTUBE_LINES) trySend(getMorningSpiritualYoutubeLinesBlock())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getMorningSpiritualYoutubeLinesBlock())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val morningSpiritualLocalVideoUrisFlow: Flow<List<String>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING_SPIRITUAL_LOCAL_VIDEO_URIS_JSON) trySend(getMorningSpiritualLocalVideoUris())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getMorningSpiritualLocalVideoUris())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val morningSpiritualPlaylistEntriesFlow: Flow<List<MorningPlaylistPref>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING_SPIRITUAL_PLAYLIST_ENTRIES_JSON) trySend(getMorningSpiritualPlaylistEntries())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getMorningSpiritualPlaylistEntries())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val showMustDoTasksFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SHOW_MUST_DO_TASKS) trySend(isShowMustDoTasksEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isShowMustDoTasksEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val showWontDoTasksFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SHOW_WONT_DO_TASKS) trySend(isShowWontDoTasksEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isShowWontDoTasksEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val taskTagColorsFlow: Flow<Map<String, Int>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_TASK_TAG_COLORS_JSON) trySend(getTaskTagColors())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getTaskTagColors())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homeMorningVideoTabFlow: Flow<MorningVideoBucket> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_MORNING_VIDEO_TAB) trySend(getHomeMorningVideoTab())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getHomeMorningVideoTab())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val appLockEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_APP_LOCK_ENABLED) trySend(isAppLockEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isAppLockEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val appLockAllowBiometricFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_APP_LOCK_ALLOW_BIOMETRIC) trySend(isAppLockBiometricAllowed())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isAppLockBiometricAllowed())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val appLockPinConfiguredFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_APP_LOCK_PIN_HASH || key == KEY_APP_LOCK_PIN_SALT) trySend(hasAppLockPin())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(hasAppLockPin())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** When true, morning motivation clips start playback as soon as the player is ready. Default off. */
    val morningMotivationAutoplayFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING_MOTIVATION_AUTOPLAY) trySend(isMorningMotivationAutoplayEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isMorningMotivationAutoplayEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homeDailyPdfUriFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_DAILY_PDF_URI) trySend(getHomeDailyPdfUri())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getHomeDailyPdfUri())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homeDailyPdfLastPageFlow: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_DAILY_PDF_LAST_PAGE) trySend(getHomeDailyPdfLastPage())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getHomeDailyPdfLastPage())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homePdfViewModeFlow: Flow<HomePdfViewMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_PDF_VIEW_MODE) trySend(getHomePdfViewMode())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getHomePdfViewMode())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homePdfThemeDarkFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_PDF_THEME_DARK) trySend(isHomePdfThemeDark())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isHomePdfThemeDark())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homePdfZoomScaleFlow: Flow<Float> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_PDF_ZOOM_SCALE) trySend(getHomePdfZoomScale())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getHomePdfZoomScale())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val homeLayoutSectionOrderFlow: Flow<List<HomeLayoutSection>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HOME_LAYOUT_SECTION_ORDER_JSON) trySend(getHomeLayoutSectionOrder())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getHomeLayoutSectionOrder())
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

    val journalContextWindowDaysFlow: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_JOURNAL_CONTEXT_WINDOW_DAYS) trySend(getJournalContextWindowDays())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getJournalContextWindowDays())
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

    val phoneUsageInChatAgentFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PHONE_USAGE_IN_CHAT_AGENT) trySend(isPhoneUsageInChatAgent())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isPhoneUsageInChatAgent())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val phoneUsageInAssistantInsightFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PHONE_USAGE_IN_ASSISTANT_INSIGHT) trySend(isPhoneUsageInAssistantInsight())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isPhoneUsageInAssistantInsight())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val phoneUsageInWeeklyGoalsInsightFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PHONE_USAGE_IN_WEEKLY_GOALS_INSIGHT) trySend(isPhoneUsageInWeeklyGoalsInsight())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isPhoneUsageInWeeklyGoalsInsight())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, enabled).apply()
    }

    fun getThemePaletteId(): String =
        prefs.getString(KEY_THEME_PALETTE, AppThemePalette.VIOLET.storageId)
            ?: AppThemePalette.VIOLET.storageId

    fun setThemePaletteId(id: String) {
        val v = AppThemePalette.fromStorageId(id).storageId
        prefs.edit().putString(KEY_THEME_PALETTE, v).apply()
    }

    fun getAppBackgroundId(): String =
        prefs.getString(KEY_APP_BACKGROUND, AppBackgroundOption.NONE.storageId)
            ?: AppBackgroundOption.NONE.storageId

    fun setAppBackgroundId(id: String) {
        val v = AppBackgroundOption.fromStorageId(id).storageId
        prefs.edit().putString(KEY_APP_BACKGROUND, v).apply()
    }

    fun getCustomWallpaperUri(): String = prefs.getString(KEY_CUSTOM_WALLPAPER_URI, "") ?: ""

    fun setCustomWallpaperUri(uri: String) {
        prefs.edit().putString(KEY_CUSTOM_WALLPAPER_URI, uri).apply()
    }

    fun clearCustomWallpaperUri() {
        prefs.edit().remove(KEY_CUSTOM_WALLPAPER_URI).apply()
    }

    fun getChatFontSizeCategoryId(): String =
        prefs.getString(KEY_CHAT_FONT_SIZE_CATEGORY, ChatFontSizeCategory.DEFAULT.storageId)
            ?: ChatFontSizeCategory.DEFAULT.storageId

    fun setChatFontSizeCategoryId(id: String) {
        val v = ChatFontSizeCategory.fromStorageId(id).storageId
        prefs.edit().putString(KEY_CHAT_FONT_SIZE_CATEGORY, v).apply()
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

    /**
     * How many calendar days of journal (ending on the context reference day) may be included.
     * **1** = that day only (default). Capped at 30.
     */
    fun getJournalContextWindowDays(): Int =
        prefs.getInt(KEY_JOURNAL_CONTEXT_WINDOW_DAYS, 1).coerceIn(1, 30)

    fun setJournalContextWindowDays(days: Int) {
        prefs.edit().putInt(KEY_JOURNAL_CONTEXT_WINDOW_DAYS, days.coerceIn(1, 30)).apply()
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

    fun getPhoneUsageInsightPrompt(): String {
        val raw = prefs.getString(KEY_PHONE_USAGE_INSIGHT_PROMPT, null)?.trim().orEmpty()
        return raw.ifEmpty { AiPromptDefaults.PHONE_USAGE_INSIGHT }
    }

    fun setPhoneUsageInsightPrompt(prompt: String) {
        prefs.edit().putString(KEY_PHONE_USAGE_INSIGHT_PROMPT, prompt).apply()
    }

    /** When true, AI Agent (chat) system context may include phone usage stats (requires usage access on device). */
    fun isPhoneUsageInChatAgent(): Boolean =
        prefs.getBoolean(KEY_PHONE_USAGE_IN_CHAT_AGENT, false)

    fun setPhoneUsageInChatAgent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PHONE_USAGE_IN_CHAT_AGENT, enabled).apply()
    }

    /** When true, home Assistant insight generation may include phone usage stats. */
    fun isPhoneUsageInAssistantInsight(): Boolean =
        prefs.getBoolean(KEY_PHONE_USAGE_IN_ASSISTANT_INSIGHT, false)

    fun setPhoneUsageInAssistantInsight(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PHONE_USAGE_IN_ASSISTANT_INSIGHT, enabled).apply()
    }

    /** When true, weekly goals insight generation may include phone usage stats. */
    fun isPhoneUsageInWeeklyGoalsInsight(): Boolean =
        prefs.getBoolean(KEY_PHONE_USAGE_IN_WEEKLY_GOALS_INSIGHT, false)

    fun setPhoneUsageInWeeklyGoalsInsight(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PHONE_USAGE_IN_WEEKLY_GOALS_INSIGHT, enabled).apply()
    }

    /**
     * Rolling/calendar window for phone usage included in AI Agent chat and Assistant insight.
     * 1 = since local midnight today; 2+ = last N×24h. Default 1, max 14.
     */
    fun getPhoneUsageAiContextDays(): Int =
        prefs.getInt(KEY_PHONE_USAGE_AI_CONTEXT_DAYS, 1).coerceIn(1, 14)

    fun setPhoneUsageAiContextDays(days: Int) {
        prefs.edit().putInt(KEY_PHONE_USAGE_AI_CONTEXT_DAYS, days.coerceIn(1, 14)).apply()
    }

    /**
     * Window for phone usage in Weekly goals insight. Default 7 days, max 30.
     */
    fun getPhoneUsageWeeklyInsightDays(): Int =
        prefs.getInt(KEY_PHONE_USAGE_WEEKLY_INSIGHT_DAYS, 7).coerceIn(1, 30)

    fun setPhoneUsageWeeklyInsightDays(days: Int) {
        prefs.edit().putInt(KEY_PHONE_USAGE_WEEKLY_INSIGHT_DAYS, days.coerceIn(1, 30)).apply()
    }

    /** Phone usage drawer: 1, 7, or 14 days (persisted). */
    fun getPhoneUsageScreenRangeDays(): Int {
        val d = prefs.getInt(KEY_PHONE_USAGE_SCREEN_RANGE_DAYS, 1)
        return when (d) {
            1, 7, 14 -> d
            else -> 1
        }
    }

    fun setPhoneUsageScreenRangeDays(days: Int) {
        val v = when (days) {
            1, 7, 14 -> days
            else -> 1
        }
        prefs.edit().putInt(KEY_PHONE_USAGE_SCREEN_RANGE_DAYS, v).apply()
    }

    fun getLlmProfiles(): List<LlmApiKeyProfile> {
        val raw = prefs.getString(KEY_LLM_PROFILES_JSON, null) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val parsed = runCatching {
            gson.fromJson<MutableList<LlmApiKeyProfile>>(raw, llmProfileListType)
        }.getOrNull()
        return parsed ?: emptyList()
    }

    fun setLlmProfiles(profiles: List<LlmApiKeyProfile>) {
        prefs.edit().putString(KEY_LLM_PROFILES_JSON, gson.toJson(ArrayList(profiles))).apply()
    }

    /**
     * OpenAI-compatible endpoints to try in order. Uses enabled profiles when any exist;
     * otherwise falls back to the legacy single Cerebras key + model.
     */
    fun llmEndpointsForFailover(): List<LlmEndpointConfig> {
        val profiles = getLlmProfiles()
        if (profiles.isNotEmpty()) {
            // When any profile exists, only enabled rows are used (no silent legacy mix-in).
            return profiles.filter { it.enabled }.mapNotNull { it.toEndpointConfig() }
        }
        val legacy = getCerebrasKey().trim()
        if (legacy.isNotEmpty()) {
            val model = getCerebrasModelId().trim().ifEmpty { DEFAULT_CEREBRAS_MODEL_ID }
            return listOf(
                LlmEndpointConfig(
                    baseUrl = "https://api.cerebras.ai/v1",
                    apiKey = legacy,
                    modelId = model,
                ),
            )
        }
        return emptyList()
    }

    fun isLlmConfigured(): Boolean = llmEndpointsForFailover().isNotEmpty()

    fun getMorningYoutubeLinesBlock(): String =
        prefs.getString(KEY_MORNING_YOUTUBE_LINES, "") ?: ""

    fun setMorningYoutubeLinesBlock(block: String) {
        prefs.edit().putString(KEY_MORNING_YOUTUBE_LINES, block).apply()
    }

    fun getMorningLocalVideoUris(): List<String> {
        val raw = prefs.getString(KEY_MORNING_LOCAL_VIDEO_URIS_JSON, null) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val parsed = runCatching {
            gson.fromJson<ArrayList<String>>(raw, stringListType)
        }.getOrNull()
        return parsed?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
    }

    fun setMorningLocalVideoUris(uris: List<String>) {
        val clean = uris.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        prefs.edit().putString(KEY_MORNING_LOCAL_VIDEO_URIS_JSON, gson.toJson(ArrayList(clean))).apply()
    }

    /**
     * Saved YouTube playlists (from watch URLs with `list=`). On first read after install,
     * two built-in playlists are written once; users can remove or edit exclusions later.
     */
    fun getMorningPlaylistEntries(): List<MorningPlaylistPref> {
        if (!prefs.getBoolean(KEY_MORNING_PLAYLIST_DEFAULTS_APPLIED, false)) {
            prefs.edit()
                .putBoolean(KEY_MORNING_PLAYLIST_DEFAULTS_APPLIED, true)
                .putString(
                    KEY_MORNING_PLAYLIST_ENTRIES_JSON,
                    gson.toJson(ArrayList(defaultMorningPlaylists)),
                )
                .apply()
            return defaultMorningPlaylists
        }
        val raw = prefs.getString(KEY_MORNING_PLAYLIST_ENTRIES_JSON, null) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val parsed = runCatching {
            gson.fromJson<ArrayList<MorningPlaylistPref>>(raw, morningPlaylistListType)
        }.getOrNull()
        return parsed?.filter { it.playlistId.isNotBlank() } ?: emptyList()
    }

    fun setMorningPlaylistEntries(entries: List<MorningPlaylistPref>) {
        val clean = entries
            .map { it.copy(playlistId = it.playlistId.trim(), sourceWatchUrl = it.sourceWatchUrl.trim()) }
            .filter { it.playlistId.isNotEmpty() }
            .distinctBy { it.playlistId }
            .map { pl ->
                pl.copy(
                    excludedVideoIds = pl.excludedVideoIds
                        .map { x -> x.trim() }
                        .filter { x -> x.length == 11 }
                        .distinct(),
                )
            }
        prefs.edit().putString(KEY_MORNING_PLAYLIST_ENTRIES_JSON, gson.toJson(ArrayList(clean))).apply()
    }

    fun isMorningMotivationAutoplayEnabled(): Boolean =
        prefs.getBoolean(KEY_MORNING_MOTIVATION_AUTOPLAY, false)

    fun setMorningMotivationAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MORNING_MOTIVATION_AUTOPLAY, enabled).apply()
    }

    /** Tasks tab: when false, must-do tasks are hidden until the user enables them in the menu. */
    fun isShowMustDoTasksEnabled(): Boolean =
        prefs.getBoolean(KEY_SHOW_MUST_DO_TASKS, false)

    fun setShowMustDoTasksEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_MUST_DO_TASKS, enabled).apply()
    }

    /** Tasks marked “won’t do” (can’t complete) are hidden unless this is on (default off). */
    fun isShowWontDoTasksEnabled(): Boolean =
        prefs.getBoolean(KEY_SHOW_WONT_DO_TASKS, false)

    fun setShowWontDoTasksEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_WONT_DO_TASKS, enabled).apply()
    }

    fun getTaskTagColors(): Map<String, Int> {
        val raw = prefs.getString(KEY_TASK_TAG_COLORS_JSON, null) ?: return emptyMap()
        @Suppress("UNCHECKED_CAST")
        val parsed = runCatching {
            gson.fromJson<HashMap<String, Int>>(raw, stringIntMapType)
        }.getOrNull()
        return parsed?.mapKeys { it.key.trim() }?.filterKeys { it.isNotEmpty() } ?: emptyMap()
    }

    fun setTaskTagColor(tagName: String, argb: Int) {
        val key = tagName.trim()
        if (key.isEmpty()) return
        val cur = getTaskTagColors().toMutableMap()
        cur[key] = argb
        prefs.edit().putString(KEY_TASK_TAG_COLORS_JSON, gson.toJson(HashMap(cur))).apply()
    }

    fun getMorningSpiritualYoutubeLinesBlock(): String =
        prefs.getString(KEY_MORNING_SPIRITUAL_YOUTUBE_LINES, "") ?: ""

    fun setMorningSpiritualYoutubeLinesBlock(block: String) {
        prefs.edit().putString(KEY_MORNING_SPIRITUAL_YOUTUBE_LINES, block).apply()
    }

    fun getMorningSpiritualLocalVideoUris(): List<String> {
        val raw = prefs.getString(KEY_MORNING_SPIRITUAL_LOCAL_VIDEO_URIS_JSON, null) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val parsed = runCatching {
            gson.fromJson<ArrayList<String>>(raw, stringListType)
        }.getOrNull()
        return parsed?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
    }

    fun setMorningSpiritualLocalVideoUris(uris: List<String>) {
        val clean = uris.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        prefs.edit().putString(KEY_MORNING_SPIRITUAL_LOCAL_VIDEO_URIS_JSON, gson.toJson(ArrayList(clean))).apply()
    }

    fun getMorningSpiritualPlaylistEntries(): List<MorningPlaylistPref> {
        val raw = prefs.getString(KEY_MORNING_SPIRITUAL_PLAYLIST_ENTRIES_JSON, null) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val parsed = runCatching {
            gson.fromJson<ArrayList<MorningPlaylistPref>>(raw, morningPlaylistListType)
        }.getOrNull()
        return parsed?.filter { it.playlistId.isNotBlank() } ?: emptyList()
    }

    fun setMorningSpiritualPlaylistEntries(entries: List<MorningPlaylistPref>) {
        val clean = entries
            .map { it.copy(playlistId = it.playlistId.trim(), sourceWatchUrl = it.sourceWatchUrl.trim()) }
            .filter { it.playlistId.isNotEmpty() }
            .distinctBy { it.playlistId }
            .map { pl ->
                pl.copy(
                    excludedVideoIds = pl.excludedVideoIds
                        .map { x -> x.trim() }
                        .filter { x -> x.length == 11 }
                        .distinct(),
                )
            }
        prefs.edit().putString(
            KEY_MORNING_SPIRITUAL_PLAYLIST_ENTRIES_JSON,
            gson.toJson(ArrayList(clean)),
        ).apply()
    }

    fun getHomeMorningVideoTab(): MorningVideoBucket {
        val raw = prefs.getString(KEY_HOME_MORNING_VIDEO_TAB, "motivation")?.trim()?.lowercase()
        return if (raw == "spiritual") MorningVideoBucket.SPIRITUAL else MorningVideoBucket.MOTIVATION
    }

    fun setHomeMorningVideoTab(tab: MorningVideoBucket) {
        prefs.edit().putString(
            KEY_HOME_MORNING_VIDEO_TAB,
            if (tab == MorningVideoBucket.SPIRITUAL) "spiritual" else "motivation",
        ).apply()
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun getAppLockPinSalt(): String = prefs.getString(KEY_APP_LOCK_PIN_SALT, "") ?: ""

    fun getAppLockPinHash(): String = prefs.getString(KEY_APP_LOCK_PIN_HASH, "") ?: ""

    fun setAppLockPin(salt: String, hashHex: String) {
        prefs.edit()
            .putString(KEY_APP_LOCK_PIN_SALT, salt)
            .putString(KEY_APP_LOCK_PIN_HASH, hashHex)
            .apply()
    }

    fun clearAppLockPin() {
        prefs.edit()
            .remove(KEY_APP_LOCK_PIN_SALT)
            .remove(KEY_APP_LOCK_PIN_HASH)
            .apply()
    }

    fun hasAppLockPin(): Boolean = getAppLockPinHash().isNotBlank() && getAppLockPinSalt().isNotBlank()

    fun isAppLockBiometricAllowed(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ALLOW_BIOMETRIC, true)

    fun setAppLockBiometricAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ALLOW_BIOMETRIC, allowed).apply()
    }

    val reminderToneKindFlow: Flow<AlertToneKind> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_REMINDER_TONE_KIND || key == KEY_REMINDER_TONE_CUSTOM_URI) {
                trySend(getReminderToneKind())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getReminderToneKind())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val reminderCustomToneUriFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_REMINDER_TONE_CUSTOM_URI) trySend(getReminderCustomToneUriString())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getReminderCustomToneUriString())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val pomodoroToneKindFlow: Flow<AlertToneKind> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_POMO_TONE_KIND || key == KEY_POMO_TONE_CUSTOM_URI) {
                trySend(getPomodoroToneKind())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getPomodoroToneKind())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val pomodoroCustomToneUriFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_POMO_TONE_CUSTOM_URI) trySend(getPomodoroCustomToneUriString())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getPomodoroCustomToneUriString())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun migrateLegacyAlertSoundIfNeeded() {
        if (prefs.contains(KEY_REMINDER_TONE_KIND)) return
        val legacy = prefs.getString(KEY_LEGACY_ALERT_SOUND_PRESET, null)
        val mapped = when (legacy) {
            "default" -> AlertToneKind.DEFAULT_NOTIFICATION
            "alarm" -> AlertToneKind.ALARM
            "silent" -> AlertToneKind.SILENT
            "vibrate" -> AlertToneKind.VIBRATE_ONLY
            else -> AlertToneKind.DEFAULT_NOTIFICATION
        }
        prefs.edit()
            .putString(KEY_REMINDER_TONE_KIND, mapped.storageId)
            .putString(KEY_POMO_TONE_KIND, mapped.storageId)
            .remove(KEY_LEGACY_ALERT_SOUND_PRESET)
            .apply()
    }

    fun getReminderToneKind(): AlertToneKind =
        AlertToneKind.fromStorageId(prefs.getString(KEY_REMINDER_TONE_KIND, null))

    fun setReminderToneKind(kind: AlertToneKind) {
        val ed = prefs.edit().putString(KEY_REMINDER_TONE_KIND, kind.storageId)
        if (kind != AlertToneKind.CUSTOM) {
            ed.remove(KEY_REMINDER_TONE_CUSTOM_URI)
        }
        ed.apply()
    }

    fun getReminderCustomToneUriString(): String =
        prefs.getString(KEY_REMINDER_TONE_CUSTOM_URI, "") ?: ""

    fun setReminderCustomToneUriString(uri: String) {
        prefs.edit().putString(KEY_REMINDER_TONE_CUSTOM_URI, uri.trim()).apply()
    }

    fun getPomodoroToneKind(): AlertToneKind =
        AlertToneKind.fromStorageId(prefs.getString(KEY_POMO_TONE_KIND, null))

    fun setPomodoroToneKind(kind: AlertToneKind) {
        val ed = prefs.edit().putString(KEY_POMO_TONE_KIND, kind.storageId)
        if (kind != AlertToneKind.CUSTOM) {
            ed.remove(KEY_POMO_TONE_CUSTOM_URI)
        }
        ed.apply()
    }

    fun getPomodoroCustomToneUriString(): String =
        prefs.getString(KEY_POMO_TONE_CUSTOM_URI, "") ?: ""

    fun setPomodoroCustomToneUriString(uri: String) {
        prefs.edit().putString(KEY_POMO_TONE_CUSTOM_URI, uri.trim()).apply()
    }

    fun getStoredNotificationTonePackId(): String =
        prefs.getString(KEY_NOTIFICATION_TONE_PACK_ID, "") ?: ""

    fun setStoredNotificationTonePackId(packId: String) {
        prefs.edit().putString(KEY_NOTIFICATION_TONE_PACK_ID, packId).apply()
    }

    fun currentNotificationTonePackId(): String = NotificationTonePack.packId(
        getReminderToneKind(),
        getReminderCustomToneUriString(),
        getPomodoroToneKind(),
        getPomodoroCustomToneUriString(),
    )

    fun reminderNotificationChannelId(): String =
        "dayroute_rem_${currentNotificationTonePackId()}"

    fun pomodoroCompleteNotificationChannelId(): String =
        "dayroute_pom_${currentNotificationTonePackId()}"

    fun getHomeDailyPdfUri(): String = prefs.getString(KEY_HOME_DAILY_PDF_URI, "") ?: ""

    fun setHomeDailyPdfUri(uri: String) {
        val t = uri.trim()
        val ed = prefs.edit().putString(KEY_HOME_DAILY_PDF_URI, t)
        if (t.isEmpty()) {
            ed.remove(KEY_HOME_DAILY_PDF_LAST_PAGE)
        } else {
            ed.putInt(KEY_HOME_DAILY_PDF_LAST_PAGE, 0)
        }
        ed.apply()
    }

    fun getHomeDailyPdfLastPage(): Int =
        prefs.getInt(KEY_HOME_DAILY_PDF_LAST_PAGE, 0).coerceAtLeast(0)

    fun setHomeDailyPdfLastPage(page: Int) {
        prefs.edit().putInt(KEY_HOME_DAILY_PDF_LAST_PAGE, page.coerceAtLeast(0)).apply()
    }

    fun getHomePdfViewMode(): HomePdfViewMode =
        HomePdfViewMode.fromStorageId(prefs.getString(KEY_HOME_PDF_VIEW_MODE, null))

    fun setHomePdfViewMode(mode: HomePdfViewMode) {
        prefs.edit().putString(KEY_HOME_PDF_VIEW_MODE, mode.storageId).apply()
    }

    fun isHomePdfThemeDark(): Boolean = prefs.getBoolean(KEY_HOME_PDF_THEME_DARK, false)

    fun setHomePdfThemeDark(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_PDF_THEME_DARK, enabled).apply()
    }

    fun getHomePdfZoomScale(): Float =
        prefs.getFloat(KEY_HOME_PDF_ZOOM_SCALE, 1f).coerceIn(0.5f, 3f)

    fun setHomePdfZoomScale(scale: Float) {
        prefs.edit().putFloat(KEY_HOME_PDF_ZOOM_SCALE, scale.coerceIn(0.5f, 3f)).apply()
    }

    fun getHomeLayoutSectionOrder(): List<HomeLayoutSection> {
        val raw = prefs.getString(KEY_HOME_LAYOUT_SECTION_ORDER_JSON, null) ?: return HomeLayoutSection.defaultOrder
        @Suppress("UNCHECKED_CAST")
        val ids = runCatching {
            gson.fromJson<ArrayList<String>>(raw, stringListType)
        }.getOrNull() ?: return HomeLayoutSection.defaultOrder
        return HomeLayoutSection.normalizeOrder(ids)
    }

    fun setHomeLayoutSectionOrder(order: List<HomeLayoutSection>) {
        val normalized = HomeLayoutSection.normalizeOrder(order.map { it.storageId })
        prefs.edit().putString(
            KEY_HOME_LAYOUT_SECTION_ORDER_JSON,
            gson.toJson(ArrayList(normalized.map { it.storageId })),
        ).apply()
    }
}
