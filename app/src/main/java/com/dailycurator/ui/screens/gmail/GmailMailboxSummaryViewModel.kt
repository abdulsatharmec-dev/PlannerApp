package com.dailycurator.ui.screens.gmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.repository.GmailMailboxSummaryRepository
import com.dailycurator.data.repository.toAiInsight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GmailMailboxSummaryUiState(
    val markdown: String = "Generate a summary to see your mailbox digest here.",
    val homeBlurb: String? = null,
    val generatedAtMillis: Long? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val rangeDays: Int = 7,
    val customDaysText: String = "14",
)

@HiltViewModel
class GmailMailboxSummaryViewModel @Inject constructor(
    private val repo: GmailMailboxSummaryRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        GmailMailboxSummaryUiState(
            rangeDays = prefs.getMailboxSummaryRangeDays(),
            customDaysText = prefs.getMailboxSummaryRangeDays().toString(),
        ),
    )
    val uiState: StateFlow<GmailMailboxSummaryUiState> = _ui.asStateFlow()

    val linkedAccounts = prefs.gmailAccountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getGmailLinkedAccounts())

    init {
        viewModelScope.launch {
            repo.observe().collect { entity ->
                if (entity == null) return@collect
                val insight = entity.toAiInsight()
                _ui.update {
                    it.copy(
                        markdown = insight.insightText,
                        homeBlurb = entity.recoveryPlan,
                        generatedAtMillis = entity.generatedAtEpochMillis,
                    )
                }
            }
        }
    }

    fun setPresetRangeDays(days: Int) {
        val d = days.coerceIn(1, 90)
        _ui.update { it.copy(rangeDays = d, error = null) }
        prefs.setMailboxSummaryRangeDays(d)
    }

    fun setCustomDaysText(text: String) {
        _ui.update { it.copy(customDaysText = text.filter { ch -> ch.isDigit() }.take(3)) }
    }

    fun applyCustomRange() {
        val d = _ui.value.customDaysText.toIntOrNull()?.coerceIn(1, 90) ?: return
        setPresetRangeDays(d)
    }

    fun regenerate() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        val result = repo.regenerate(_ui.value.rangeDays)
        _ui.update {
            it.copy(
                loading = false,
                error = result.exceptionOrNull()?.message,
            )
        }
    }
}
