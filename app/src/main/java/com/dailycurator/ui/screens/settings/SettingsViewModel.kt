package com.dailycurator.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.DayWindowMinutes
import com.dailycurator.data.local.CEREBRAS_MODEL_OPTIONS
import com.dailycurator.data.local.CerebrasModelOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = prefs.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isDarkTheme())

    val assistantInsightEnabled: StateFlow<Boolean> = prefs.assistantInsightEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isAssistantInsightEnabled())

    val weeklyGoalsInsightEnabled: StateFlow<Boolean> = prefs.weeklyGoalsInsightEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isWeeklyGoalsInsightEnabled())

    private val _cerebrasKey = MutableStateFlow(prefs.getCerebrasKey())
    val cerebrasKey = _cerebrasKey.asStateFlow()

    private val _cerebrasModelId = MutableStateFlow(prefs.getCerebrasModelId())
    val cerebrasModelId = _cerebrasModelId.asStateFlow()

    private val _assistantInsightPrompt = MutableStateFlow(prefs.getAssistantInsightPrompt())
    val assistantInsightPrompt = _assistantInsightPrompt.asStateFlow()

    private val _weeklyGoalsInsightPrompt = MutableStateFlow(prefs.getWeeklyGoalsInsightPrompt())
    val weeklyGoalsInsightPrompt = _weeklyGoalsInsightPrompt.asStateFlow()

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
}
