package com.dailycurator.ui.screens.gmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.gmail.GmailSummarySuggestedTask
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.repository.GmailMailboxSummaryRepository
import com.dailycurator.data.repository.TaskRepository
import com.dailycurator.data.repository.toAiInsight
import com.dailycurator.reminders.TaskReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

private val PLACEHOLDER_MARKDOWN = "Generate a summary to see your mailbox digest here."

data class GmailMailboxSummaryUiState(
    val markdown: String = PLACEHOLDER_MARKDOWN,
    val homeBlurb: String? = null,
    val generatedAtMillis: Long? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val rangeDays: Int = 7,
    val customDaysText: String = "14",
    val suggestTasksLoading: Boolean = false,
    val suggestTasksBanner: String? = null,
    val showSuggestTasksDialog: Boolean = false,
    val suggestedTasks: List<GmailSummarySuggestedTask> = emptyList(),
    /** Indices of [suggestedTasks] the user wants to add. */
    val suggestedTaskSelected: Set<Int> = emptySet(),
)

@HiltViewModel
class GmailMailboxSummaryViewModel @Inject constructor(
    private val repo: GmailMailboxSummaryRepository,
    private val prefs: AppPreferences,
    private val taskRepo: TaskRepository,
    private val taskReminderScheduler: TaskReminderScheduler,
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

    fun dismissSuggestTasksDialog() {
        _ui.update {
            it.copy(
                showSuggestTasksDialog = false,
                suggestedTasks = emptyList(),
                suggestedTaskSelected = emptySet(),
            )
        }
    }

    fun toggleSuggestedTask(index: Int) {
        _ui.update { s ->
            val next = s.suggestedTaskSelected.toMutableSet()
            if (index in next) next.remove(index) else next.add(index)
            s.copy(suggestedTaskSelected = next)
        }
    }

    fun selectAllSuggestedTasks(select: Boolean) {
        _ui.update { s ->
            val idx = if (select) s.suggestedTasks.indices.toSet() else emptySet()
            s.copy(suggestedTaskSelected = idx)
        }
    }

    fun findTasksFromSummary() = viewModelScope.launch {
        val md = _ui.value.markdown.trim()
        if (md.isEmpty() || md == PLACEHOLDER_MARKDOWN) {
            _ui.update {
                it.copy(suggestTasksBanner = "Generate a summary first, then try again.")
            }
            return@launch
        }
        _ui.update {
            it.copy(
                suggestTasksLoading = true,
                suggestTasksBanner = null,
            )
        }
        val result = repo.suggestTasksFromSummaryMarkdown(md)
        result.fold(
            onSuccess = { list ->
                if (list.isEmpty()) {
                    _ui.update {
                        it.copy(
                            suggestTasksLoading = false,
                            suggestTasksBanner = "No actionable tasks found in this summary.",
                        )
                    }
                } else {
                    _ui.update {
                        it.copy(
                            suggestTasksLoading = false,
                            suggestedTasks = list,
                            suggestedTaskSelected = list.indices.toSet(),
                            showSuggestTasksDialog = true,
                        )
                    }
                }
            },
            onFailure = { e ->
                _ui.update {
                    it.copy(
                        suggestTasksLoading = false,
                        suggestTasksBanner = e.message ?: "Could not suggest tasks.",
                    )
                }
            },
        )
    }

    fun confirmAddSuggestedTasks() = viewModelScope.launch {
        val state = _ui.value
        val today = LocalDate.now()
        val indices = state.suggestedTaskSelected.sorted()
        if (indices.isEmpty()) {
            dismissSuggestTasksDialog()
            return@launch
        }
        val tasks = indices.mapNotNull { i -> state.suggestedTasks.getOrNull(i) }
        var rankBase = taskRepo.getTasksForDate(today).first().maxOfOrNull { it.rank } ?: 0
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(10, 0)
        for (t in tasks) {
            rankBase += 1
            val newId = taskRepo.insert(
                PriorityTask(
                    rank = rankBase,
                    title = t.title,
                    startTime = start,
                    endTime = end,
                    dueInfo = t.detail.takeIf { it.isNotBlank() },
                    urgency = t.urgency,
                    isTopFive = false,
                    date = today,
                ),
            )
            taskRepo.getById(newId)?.let { taskReminderScheduler.schedule(it) }
        }
        dismissSuggestTasksDialog()
        _ui.update { it.copy(suggestTasksBanner = "Added ${tasks.size} task(s) for today.") }
    }
}
