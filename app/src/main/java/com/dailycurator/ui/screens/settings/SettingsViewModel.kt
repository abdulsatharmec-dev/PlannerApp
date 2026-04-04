package com.dailycurator.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = prefs.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.isDarkTheme())

    fun toggleDarkTheme() = prefs.setDarkTheme(!isDarkTheme.value)
}
