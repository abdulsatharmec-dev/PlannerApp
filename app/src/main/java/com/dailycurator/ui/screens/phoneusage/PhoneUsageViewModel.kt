package com.dailycurator.ui.screens.phoneusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.model.AiInsight
import com.dailycurator.data.repository.PhoneUsageRepository
import com.dailycurator.data.usage.PhoneUsageSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhoneUsageUiState(
    val hasPermission: Boolean = false,
    val rangeDays: Int = 1,
    val loadingStats: Boolean = false,
    val snapshot: PhoneUsageSnapshot? = null,
    val insight: AiInsight? = null,
    val generatingInsight: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PhoneUsageViewModel @Inject constructor(
    private val repo: PhoneUsageRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    val cerebrasKeyPresent: StateFlow<Boolean> = prefs.cerebrasKeyPresentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getCerebrasKey().isNotBlank())

    private val _uiState = MutableStateFlow(
        PhoneUsageUiState(rangeDays = prefs.getPhoneUsageScreenRangeDays()),
    )
    val uiState: StateFlow<PhoneUsageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val allowed = repo.hasUsageStatsPermission()
            _uiState.update { it.copy(hasPermission = allowed) }
            if (allowed) loadStatsInternal()
        }
    }

    fun onResume() {
        val allowed = repo.hasUsageStatsPermission()
        val was = _uiState.value.hasPermission
        _uiState.update { it.copy(hasPermission = allowed) }
        if (allowed && !was) {
            viewModelScope.launch { loadStatsInternal() }
        }
    }

    fun setRangeDays(days: Int) {
        val d = when (days) {
            1, 7, 14 -> days
            else -> 1
        }
        prefs.setPhoneUsageScreenRangeDays(d)
        _uiState.update { it.copy(rangeDays = d, insight = null) }
        if (_uiState.value.hasPermission) {
            viewModelScope.launch { loadStatsInternal() }
        }
    }

    fun refreshStats() {
        if (!_uiState.value.hasPermission) return
        viewModelScope.launch { loadStatsInternal() }
    }

    fun generateInsight() = viewModelScope.launch {
        val snap = _uiState.value.snapshot ?: return@launch
        _uiState.update { it.copy(generatingInsight = true, error = null) }
        repo.generateUsageInsight(snap).fold(
            onSuccess = { insight ->
                _uiState.update { it.copy(generatingInsight = false, insight = insight, error = null) }
            },
            onFailure = { e ->
                _uiState.update {
                    it.copy(
                        generatingInsight = false,
                        error = e.message ?: "Could not generate summary.",
                    )
                }
            },
        )
    }

    private suspend fun loadStatsInternal() {
        _uiState.update { it.copy(loadingStats = true, error = null) }
        runCatching { repo.loadSnapshot(_uiState.value.rangeDays) }
            .onSuccess { snap ->
                _uiState.update { it.copy(loadingStats = false, snapshot = snap, error = null) }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        loadingStats = false,
                        error = e.message ?: "Could not load usage stats.",
                    )
                }
            }
    }
}
