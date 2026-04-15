package com.dailycurator.ui.screens.journal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.JournalEntry
import com.dailycurator.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalEditorViewModel @Inject constructor(
    private val repo: JournalRepository,
    @ApplicationContext private val appContext: Context,
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

    private val _isEvergreen = MutableStateFlow(false)
    val isEvergreen: StateFlow<Boolean> = _isEvergreen.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _voiceRelativePath = MutableStateFlow<String?>(null)
    val voiceRelativePath: StateFlow<String?> = _voiceRelativePath.asStateFlow()

    val isNewEntry: Boolean get() = entryId == 0L

    private var saveCommitted = false

    init {
        viewModelScope.launch {
            if (entryId != 0L) {
                repo.getById(entryId)?.let {
                    _title.value = it.title
                    _body.value = it.body
                    _includeInAgentChat.value = it.includeInAgentChat
                    _includeInAssistantInsight.value = it.includeInAssistantInsight
                    _includeInWeeklyGoalsInsight.value = it.includeInWeeklyGoalsInsight
                    _isEvergreen.value = it.isEvergreen
                    _voiceRelativePath.value = it.voiceRelativePath
                }
            }
            _ready.value = true
        }
    }

    /** After a successful recording, attach the new file and remove any previous attachment file. */
    fun replaceVoiceAttachment(relativePath: String) {
        val old = _voiceRelativePath.value
        if (old != null && old != relativePath) {
            JournalVoiceFiles.deleteIfExists(appContext, old)
        }
        _voiceRelativePath.value = relativePath
    }

    fun clearVoiceAttachment() {
        JournalVoiceFiles.deleteIfExists(appContext, _voiceRelativePath.value)
        _voiceRelativePath.value = null
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

    fun setEvergreen(value: Boolean) {
        _isEvergreen.value = value
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
                    isEvergreen = _isEvergreen.value,
                    voiceRelativePath = _voiceRelativePath.value,
                ),
            )
            saveCommitted = true
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
                    isEvergreen = _isEvergreen.value,
                    voiceRelativePath = _voiceRelativePath.value,
                ),
            )
            saveCommitted = true
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        if (entryId == 0L) return@launch
        val existing = repo.getById(entryId) ?: return@launch
        JournalVoiceFiles.deleteIfExists(appContext, existing.voiceRelativePath)
        repo.delete(existing)
        saveCommitted = true
        onDone()
    }

    override fun onCleared() {
        if (isNewEntry && !saveCommitted) {
            JournalVoiceFiles.deleteIfExists(appContext, _voiceRelativePath.value)
        }
        super.onCleared()
    }
}
