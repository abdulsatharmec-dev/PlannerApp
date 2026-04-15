package com.dailycurator.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.gmail.GmailLinkedAccountPref
import com.dailycurator.data.gmail.GmailTokenProvider
import com.dailycurator.data.gmail.GmailTokenResult
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.ChatFontSizeCategory
import com.dailycurator.data.media.MorningPlaylistPref
import com.dailycurator.data.media.MorningVideoBucket
import com.dailycurator.data.media.YoutubePlaylistIdExtractor
import com.dailycurator.data.media.YoutubePlaylistVideoIdsFetcher
import com.dailycurator.data.local.DayWindowMinutes
import com.dailycurator.data.local.CEREBRAS_MODEL_OPTIONS
import com.dailycurator.data.local.CerebrasModelOption
import com.dailycurator.data.local.LlmApiKeyProfile
import com.dailycurator.ui.theme.AppBackgroundOption
import com.dailycurator.security.AppPinHasher
import com.dailycurator.ui.theme.AppThemePalette
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import android.os.Build
import android.provider.MediaStore
import android.content.ContentUris
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val gmailTokenProvider: GmailTokenProvider,
    private val youtubePlaylistVideoIdsFetcher: YoutubePlaylistVideoIdsFetcher,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = prefs.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isDarkTheme())

    val themePaletteId: StateFlow<String> = prefs.themePaletteIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getThemePaletteId())

    val appBackgroundId: StateFlow<String> = prefs.appBackgroundIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getAppBackgroundId())

    val customWallpaperUri: StateFlow<String> = prefs.customWallpaperUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getCustomWallpaperUri())

    val chatFontSizeCategoryId: StateFlow<String> = prefs.chatFontSizeCategoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getChatFontSizeCategoryId())

    val assistantInsightEnabled: StateFlow<Boolean> = prefs.assistantInsightEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isAssistantInsightEnabled())

    val weeklyGoalsInsightEnabled: StateFlow<Boolean> = prefs.weeklyGoalsInsightEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isWeeklyGoalsInsightEnabled())

    val journalShareWithChat: StateFlow<Boolean> = prefs.journalShareWithChatFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isJournalSharedWithChat())

    val journalInAssistantInsight: StateFlow<Boolean> = prefs.journalInAssistantInsightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isJournalInAssistantInsight())

    val journalInWeeklyGoalsInsight: StateFlow<Boolean> = prefs.journalInWeeklyGoalsInsightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isJournalInWeeklyGoalsInsight())

    val journalContextWindowDays: StateFlow<Int> = prefs.journalContextWindowDaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getJournalContextWindowDays())

    val phoneUsageInChatAgent: StateFlow<Boolean> = prefs.phoneUsageInChatAgentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isPhoneUsageInChatAgent())

    val phoneUsageInAssistantInsight: StateFlow<Boolean> = prefs.phoneUsageInAssistantInsightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isPhoneUsageInAssistantInsight())

    val phoneUsageInWeeklyGoalsInsight: StateFlow<Boolean> = prefs.phoneUsageInWeeklyGoalsInsightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isPhoneUsageInWeeklyGoalsInsight())

    private val _phoneUsageAiContextDays = MutableStateFlow(prefs.getPhoneUsageAiContextDays())
    val phoneUsageAiContextDays = _phoneUsageAiContextDays.asStateFlow()

    private val _phoneUsageWeeklyInsightDays = MutableStateFlow(prefs.getPhoneUsageWeeklyInsightDays())
    val phoneUsageWeeklyInsightDays = _phoneUsageWeeklyInsightDays.asStateFlow()

    private val _cerebrasKey = MutableStateFlow(prefs.getCerebrasKey())
    val cerebrasKey = _cerebrasKey.asStateFlow()

    private val _cerebrasModelId = MutableStateFlow(prefs.getCerebrasModelId())
    val cerebrasModelId = _cerebrasModelId.asStateFlow()

    private val _assistantInsightPrompt = MutableStateFlow(prefs.getAssistantInsightPrompt())
    val assistantInsightPrompt = _assistantInsightPrompt.asStateFlow()

    private val _weeklyGoalsInsightPrompt = MutableStateFlow(prefs.getWeeklyGoalsInsightPrompt())
    val weeklyGoalsInsightPrompt = _weeklyGoalsInsightPrompt.asStateFlow()

    private val _phoneUsageInsightPrompt = MutableStateFlow(prefs.getPhoneUsageInsightPrompt())
    val phoneUsageInsightPrompt = _phoneUsageInsightPrompt.asStateFlow()

    val gmailAccounts = prefs.gmailAccountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getGmailLinkedAccounts())

    val agentGmailReadEnabled: StateFlow<Boolean> = prefs.agentGmailReadEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isAgentGmailReadEnabled())

    val agentGmailSendEnabled: StateFlow<Boolean> = prefs.agentGmailSendEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isAgentGmailSendEnabled())

    val homeGmailSummaryEnabled: StateFlow<Boolean> = prefs.homeGmailSummaryEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isHomeGmailSummaryEnabled())

    val agentMemoryEnabled: StateFlow<Boolean> = prefs.agentMemoryEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isAgentMemoryEnabled())

    private val _gmailMailboxSummaryPrompt = MutableStateFlow(prefs.getGmailMailboxSummaryPrompt())
    val gmailMailboxSummaryPrompt = _gmailMailboxSummaryPrompt.asStateFlow()

    private val _memoryExtractionPrompt = MutableStateFlow(prefs.getMemoryExtractionPrompt())
    val memoryExtractionPrompt = _memoryExtractionPrompt.asStateFlow()

    val cerebrasModelChoices: List<CerebrasModelOption> = CEREBRAS_MODEL_OPTIONS

    val dayWindow: StateFlow<DayWindowMinutes> = prefs.dayWindowFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getDayWindow())

    val llmProfiles: StateFlow<List<LlmApiKeyProfile>> = prefs.llmProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getLlmProfiles())

    val morningYoutubeLinesBlock: StateFlow<String> = prefs.morningYoutubeLinesBlockFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getMorningYoutubeLinesBlock())

    val morningLocalVideoUris: StateFlow<List<String>> = prefs.morningLocalVideoUrisFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getMorningLocalVideoUris())

    val morningPlaylistEntries: StateFlow<List<MorningPlaylistPref>> = prefs.morningPlaylistEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getMorningPlaylistEntries())

    val morningSpiritualYoutubeLinesBlock: StateFlow<String> = prefs.morningSpiritualYoutubeLinesBlockFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getMorningSpiritualYoutubeLinesBlock())

    val morningSpiritualLocalVideoUris: StateFlow<List<String>> = prefs.morningSpiritualLocalVideoUrisFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getMorningSpiritualLocalVideoUris())

    val morningSpiritualPlaylistEntries: StateFlow<List<MorningPlaylistPref>> =
        prefs.morningSpiritualPlaylistEntriesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getMorningSpiritualPlaylistEntries())

    val appLockEnabled: StateFlow<Boolean> = prefs.appLockEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isAppLockEnabled())

    val appLockAllowBiometric: StateFlow<Boolean> = prefs.appLockAllowBiometricFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isAppLockBiometricAllowed())

    val appLockPinConfigured: StateFlow<Boolean> = prefs.appLockPinConfiguredFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.hasAppLockPin())

    val morningMotivationAutoplay: StateFlow<Boolean> = prefs.morningMotivationAutoplayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isMorningMotivationAutoplayEnabled())

    val homeDailyPdfUri: StateFlow<String> = prefs.homeDailyPdfUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getHomeDailyPdfUri())

    fun toggleDarkTheme() = prefs.setDarkTheme(!isDarkTheme.value)

    fun setThemePalette(palette: AppThemePalette) {
        prefs.setThemePaletteId(palette.storageId)
    }

    fun setAppBackground(option: AppBackgroundOption) {
        prefs.setAppBackgroundId(option.storageId)
    }

    fun setCustomWallpaperUri(uri: String) {
        prefs.setCustomWallpaperUri(uri)
    }

    fun setChatFontSizeCategory(category: ChatFontSizeCategory) {
        prefs.setChatFontSizeCategoryId(category.storageId)
    }

    fun clearCustomWallpaper() {
        val cur = prefs.getCustomWallpaperUri()
        if (cur.isNotBlank()) {
            runCatching {
                val u = Uri.parse(cur)
                appContext.contentResolver.releasePersistableUriPermission(
                    u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        prefs.clearCustomWallpaperUri()
    }

    fun setDayWindowStart(time: LocalTime) {
        prefs.setDayWindowStartMinutes(time.hour * 60 + time.minute)
    }

    fun setDayWindowEnd(time: LocalTime) {
        prefs.setDayWindowEndMinutes(time.hour * 60 + time.minute)
    }

    fun setAssistantInsightEnabled(enabled: Boolean) {
        prefs.setAssistantInsightEnabled(enabled)
    }

    fun setWeeklyGoalsInsightEnabled(enabled: Boolean) {
        prefs.setWeeklyGoalsInsightEnabled(enabled)
    }

    fun setJournalShareWithChat(enabled: Boolean) {
        prefs.setJournalSharedWithChat(enabled)
    }

    fun setJournalInAssistantInsight(enabled: Boolean) {
        prefs.setJournalInAssistantInsight(enabled)
    }

    fun setJournalInWeeklyGoalsInsight(enabled: Boolean) {
        prefs.setJournalInWeeklyGoalsInsight(enabled)
    }

    fun setJournalContextWindowDays(days: Int) {
        prefs.setJournalContextWindowDays(days)
    }

    fun persistAssistantPrompt(text: String) {
        prefs.setAssistantInsightPrompt(text.trim())
        _assistantInsightPrompt.value = prefs.getAssistantInsightPrompt()
    }

    fun persistWeeklyGoalsPrompt(text: String) {
        prefs.setWeeklyGoalsInsightPrompt(text.trim())
        _weeklyGoalsInsightPrompt.value = prefs.getWeeklyGoalsInsightPrompt()
    }

    fun setPhoneUsageInChatAgent(enabled: Boolean) {
        prefs.setPhoneUsageInChatAgent(enabled)
    }

    fun setPhoneUsageInAssistantInsight(enabled: Boolean) {
        prefs.setPhoneUsageInAssistantInsight(enabled)
    }

    fun setPhoneUsageInWeeklyGoalsInsight(enabled: Boolean) {
        prefs.setPhoneUsageInWeeklyGoalsInsight(enabled)
    }

    fun persistPhoneUsageInsightPrompt(text: String) {
        prefs.setPhoneUsageInsightPrompt(text.trim())
        _phoneUsageInsightPrompt.value = prefs.getPhoneUsageInsightPrompt()
    }

    fun setPhoneUsageAiContextDays(days: Int) {
        prefs.setPhoneUsageAiContextDays(days)
        _phoneUsageAiContextDays.value = prefs.getPhoneUsageAiContextDays()
    }

    fun setPhoneUsageWeeklyInsightDays(days: Int) {
        prefs.setPhoneUsageWeeklyInsightDays(days)
        _phoneUsageWeeklyInsightDays.value = prefs.getPhoneUsageWeeklyInsightDays()
    }

    fun onCerebrasKeyChange(newKey: String) {
        _cerebrasKey.value = newKey
    }

    fun saveCerebrasKey() {
        prefs.setCerebrasKey(_cerebrasKey.value)
    }

    fun onCerebrasModelSelected(modelId: String) {
        prefs.setCerebrasModelId(modelId)
        _cerebrasModelId.value = prefs.getCerebrasModelId()
    }

    fun onGmailLinked(email: String) {
        if (email.isBlank()) return
        prefs.upsertGmailLinkedAccount(GmailLinkedAccountPref(email = email.trim(), showInSummary = true))
    }

    /**
     * Tries to obtain a Gmail access token; on success the account is already authorized.
     * If a consent / browser screen is required, returns [GmailTokenResult.NeedsUserInteraction].
     */
    fun startGmailAccessProbe(email: String, onResult: (GmailTokenResult) -> Unit) {
        viewModelScope.launch {
            val r = gmailTokenProvider.getAccessToken(email.trim())
            withContext(Dispatchers.Main) { onResult(r) }
        }
    }

    fun removeGmailAccount(email: String) {
        prefs.removeGmailLinkedAccount(email)
    }

    fun setGmailSummaryVisible(email: String, visible: Boolean) {
        prefs.setGmailSummaryVisible(email, visible)
    }

    fun setAgentGmailReadEnabled(enabled: Boolean) {
        prefs.setAgentGmailReadEnabled(enabled)
    }

    fun setAgentGmailSendEnabled(enabled: Boolean) {
        prefs.setAgentGmailSendEnabled(enabled)
    }

    fun setHomeGmailSummaryEnabled(enabled: Boolean) {
        prefs.setHomeGmailSummaryEnabled(enabled)
    }

    fun setAgentMemoryEnabled(enabled: Boolean) {
        prefs.setAgentMemoryEnabled(enabled)
    }

    fun persistGmailMailboxSummaryPrompt(text: String) {
        prefs.setGmailMailboxSummaryPrompt(text.trim())
        _gmailMailboxSummaryPrompt.value = prefs.getGmailMailboxSummaryPrompt()
    }

    fun persistMemoryExtractionPrompt(text: String) {
        prefs.setMemoryExtractionPrompt(text.trim())
        _memoryExtractionPrompt.value = prefs.getMemoryExtractionPrompt()
    }

    fun upsertLlmProfile(profile: LlmApiKeyProfile) {
        val cur = prefs.getLlmProfiles().toMutableList()
        val i = cur.indexOfFirst { it.id == profile.id }
        if (i >= 0) cur[i] = profile else cur.add(profile)
        prefs.setLlmProfiles(cur)
    }

    fun removeLlmProfile(id: String) {
        prefs.setLlmProfiles(prefs.getLlmProfiles().filterNot { it.id == id })
    }

    fun setLlmProfileEnabled(id: String, enabled: Boolean) {
        prefs.setLlmProfiles(
            prefs.getLlmProfiles().map { p ->
                if (p.id == id) p.copy(enabled = enabled) else p
            },
        )
    }

    fun setMorningYoutubeLinesBlock(block: String) {
        prefs.setMorningYoutubeLinesBlock(block)
    }

    fun setMorningSpiritualYoutubeLinesBlock(block: String) {
        prefs.setMorningSpiritualYoutubeLinesBlock(block)
    }

    fun setMorningLocalVideoUris(uris: List<String>) {
        prefs.setMorningLocalVideoUris(uris)
    }

    fun setMorningLocalVideoUrisForBucket(bucket: MorningVideoBucket, uris: List<String>) {
        when (bucket) {
            MorningVideoBucket.MOTIVATION -> prefs.setMorningLocalVideoUris(uris)
            MorningVideoBucket.SPIRITUAL -> prefs.setMorningSpiritualLocalVideoUris(uris)
        }
    }

    fun setMorningPlaylistEntries(entries: List<MorningPlaylistPref>) {
        prefs.setMorningPlaylistEntries(entries)
    }

    fun setMorningSpiritualPlaylistEntries(entries: List<MorningPlaylistPref>) {
        prefs.setMorningSpiritualPlaylistEntries(entries)
    }

    fun addMorningPlaylistFromWatchUrl(url: String, bucket: MorningVideoBucket): Boolean {
        val pid = YoutubePlaylistIdExtractor.extractPlaylistIdFromWatchUrl(url) ?: return false
        return when (bucket) {
            MorningVideoBucket.MOTIVATION -> {
                val cur = prefs.getMorningPlaylistEntries().toMutableList()
                if (cur.any { it.playlistId == pid }) return false
                cur.add(MorningPlaylistPref(playlistId = pid, sourceWatchUrl = url.trim()))
                prefs.setMorningPlaylistEntries(cur)
                true
            }
            MorningVideoBucket.SPIRITUAL -> {
                val cur = prefs.getMorningSpiritualPlaylistEntries().toMutableList()
                if (cur.any { it.playlistId == pid }) return false
                cur.add(MorningPlaylistPref(playlistId = pid, sourceWatchUrl = url.trim()))
                prefs.setMorningSpiritualPlaylistEntries(cur)
                true
            }
        }
    }

    fun removeMorningPlaylist(playlistId: String, bucket: MorningVideoBucket) {
        when (bucket) {
            MorningVideoBucket.MOTIVATION -> prefs.setMorningPlaylistEntries(
                prefs.getMorningPlaylistEntries().filterNot { it.playlistId == playlistId },
            )
            MorningVideoBucket.SPIRITUAL -> prefs.setMorningSpiritualPlaylistEntries(
                prefs.getMorningSpiritualPlaylistEntries().filterNot { it.playlistId == playlistId },
            )
        }
    }

    fun updateMorningPlaylistExcluded(playlistId: String, excludedVideoIds: List<String>, bucket: MorningVideoBucket) {
        when (bucket) {
            MorningVideoBucket.MOTIVATION -> {
                val cur = prefs.getMorningPlaylistEntries().map { pl ->
                    if (pl.playlistId == playlistId) pl.copy(excludedVideoIds = excludedVideoIds.distinct()) else pl
                }
                prefs.setMorningPlaylistEntries(cur)
            }
            MorningVideoBucket.SPIRITUAL -> {
                val cur = prefs.getMorningSpiritualPlaylistEntries().map { pl ->
                    if (pl.playlistId == playlistId) pl.copy(excludedVideoIds = excludedVideoIds.distinct()) else pl
                }
                prefs.setMorningSpiritualPlaylistEntries(cur)
            }
        }
    }

    suspend fun fetchPlaylistVideoIdsForEditor(playlistId: String): List<String> =
        youtubePlaylistVideoIdsFetcher.fetchOrderedVideoIds(playlistId, forceRefresh = true)

    suspend fun queryDeviceVideosForPicker(): List<DeviceVideoListItem> = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
        )
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val list = ArrayList<DeviceVideoListItem>(256)
        resolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sort,
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol)?.trim().orEmpty().ifEmpty { "Video $id" }
                val dur = c.getLong(durCol).takeIf { it > 0L }
                list.add(DeviceVideoListItem(id = id, displayName = name, durationMs = dur))
            }
        }
        list
    }

    fun contentUriStringForVideoMediaId(id: Long): String =
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

    fun videoReadPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun setMorningMotivationAutoplay(enabled: Boolean) {
        prefs.setMorningMotivationAutoplayEnabled(enabled)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.setAppLockEnabled(enabled)
        if (!enabled) {
            prefs.clearAppLockPin()
        }
    }

    fun setAppLockAllowBiometric(allowed: Boolean) {
        prefs.setAppLockBiometricAllowed(allowed)
    }

    fun setAppLockPin(pin: String) {
        val salt = AppPinHasher.randomSaltHex()
        val hash = AppPinHasher.sha256Hex(salt, pin)
        prefs.setAppLockPin(salt, hash)
    }

    fun verifyAppLockPin(pin: String): Boolean {
        val salt = prefs.getAppLockPinSalt()
        val hash = prefs.getAppLockPinHash()
        if (salt.isBlank() || hash.isBlank()) return false
        return AppPinHasher.sha256Hex(salt, pin) == hash
    }

    fun setHomeDailyPdfUri(uri: String) {
        prefs.setHomeDailyPdfUri(uri)
    }

    fun clearHomeDailyPdf() {
        prefs.setHomeDailyPdfUri("")
    }
}

data class DeviceVideoListItem(
    val id: Long,
    val displayName: String,
    val durationMs: Long?,
)
