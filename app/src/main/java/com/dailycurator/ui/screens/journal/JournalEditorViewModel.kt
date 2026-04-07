package com.dailycurator.ui.screens.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.JournalEntry
import com.dailycurator.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalEditorViewModel @Inject constructor(
    private val repo: JournalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private val _includeInAgentChat = MutableStateFlow(true)
    val includeInAgentChat: StateFlow<Boolean> = _includeInAgentChat.asStateFlow()

    private val _includeInAssistantInsight = MutableStateFlow(true)
    val includeInAssistantInsight: StateFlow<Boolean> = _includeInAssistantInsight.asStateFlow()

    private val _includeInWeeklyGoalsInsight = MutableStateFlow(true)
    val includeInWeeklyGoalsInsight: StateFlow<Boolean> = _includeInWeeklyGoalsInsight.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val isNewEntry: Boolean get() = entryId == 0L

    init {
        viewModelScope.launch {
            if (entryId != 0L) {
                repo.getById(entryId)?.let {
                    _title.value = it.title
                    _body.value = it.body
                    _includeInAgentChat.value = it.includeInAgentChat
                    _includeInAssistantInsight.value = it.includeInAssistantInsight
                    _includeInWeeklyGoalsInsight.value = it.includeInWeeklyGoalsInsight
                }
            }
            _ready.value = true
        }
    }

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setBody(value: String) {
        _body.value = value
    }

    fun setIncludeInAgentChat(value: Boolean) {
        _includeInAgentChat.value = value
    }

    fun setIncludeInAssistantInsight(value: Boolean) {
        _includeInAssistantInsight.value = value
    }

    fun setIncludeInWeeklyGoalsInsight(value: Boolean) {
        _includeInWeeklyGoalsInsight.value = value
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val t = derivedTitle(_title.value, _body.value)
        val bodyTrim = _body.value.trim()
        if (entryId == 0L) {
            repo.insert(
                JournalEntry(
                    title = t,
                    body = bodyTrim,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    includeInAgentChat = _includeInAgentChat.value,
                    includeInAssistantInsight = _includeInAssistantInsight.value,
                    includeInWeeklyGoalsInsight = _includeInWeeklyGoalsInsight.value,
                ),
            )
        } else {
            val existing = repo.getById(entryId) ?: return@launch
            repo.update(
                existing.copy(
                    title = t,
                    body = bodyTrim,
                    updatedAtEpochMillis = now,
                    includeInAgentChat = _includeInAgentChat.value,
                    includeInAssistantInsight = _includeInAssistantInsight.value,
                    includeInWeeklyGoalsInsight = _includeInWeeklyGoalsInsight.value,
                ),
            )
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        if (entryId == 0L) return@launch
        val existing = repo.getById(entryId) ?: return@launch
        repo.delete(existing)
        onDone()
    }
}
