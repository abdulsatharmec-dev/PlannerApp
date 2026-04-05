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

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val isNewEntry: Boolean get() = entryId == 0L

    init {
        viewModelScope.launch {
            if (entryId != 0L) {
                repo.getById(entryId)?.let {
                    _title.value = it.title
                    _body.value = it.body
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
                ),
            )
        } else {
            val existing = repo.getById(entryId) ?: return@launch
            repo.update(
                existing.copy(
                    title = t,
                    body = bodyTrim,
                    updatedAtEpochMillis = now,
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
