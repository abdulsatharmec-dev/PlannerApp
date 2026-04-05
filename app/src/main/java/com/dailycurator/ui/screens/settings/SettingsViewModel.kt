package com.dailycurator.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.gmail.GmailLinkedAccountPref
import com.dailycurator.data.gmail.GmailTokenProvider
import com.dailycurator.data.gmail.GmailTokenResult
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.DayWindowMinutes
import com.dailycurator.data.local.CEREBRAS_MODEL_OPTIONS
import com.dailycurator.data.local.CerebrasModelOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val gmailTokenProvider: GmailTokenProvider,
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = prefs.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isDarkTheme())

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

    private val _cerebrasKey = MutableStateFlow(prefs.getCerebrasKey())
    val cerebrasKey = _cerebrasKey.asStateFlow()

    private val _cerebrasModelId = MutableStateFlow(prefs.getCerebrasModelId())
    val cerebrasModelId = _cerebrasModelId.asStateFlow()

    private val _assistantInsightPrompt = MutableStateFlow(prefs.getAssistantInsightPrompt())
    val assistantInsightPrompt = _assistantInsightPrompt.asStateFlow()

    private val _weeklyGoalsInsightPrompt = MutableStateFlow(prefs.getWeeklyGoalsInsightPrompt())
    val weeklyGoalsInsightPrompt = _weeklyGoalsInsightPrompt.asStateFlow()

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

    fun toggleDarkTheme() = prefs.setDarkTheme(!isDarkTheme.value)

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

    fun persistAssistantPrompt(text: String) {
        prefs.setAssistantInsightPrompt(text.trim())
        _assistantInsightPrompt.value = prefs.getAssistantInsightPrompt()
    }

    fun persistWeeklyGoalsPrompt(text: String) {
        prefs.setWeeklyGoalsInsightPrompt(text.trim())
        _weeklyGoalsInsightPrompt.value = prefs.getWeeklyGoalsInsightPrompt()
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
}
