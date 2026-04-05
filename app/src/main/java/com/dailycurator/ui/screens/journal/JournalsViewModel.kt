package com.dailycurator.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.JournalEntry
import com.dailycurator.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalsViewModel @Inject constructor(
    private val repo: JournalRepository,
) : ViewModel() {

    val entries: StateFlow<List<JournalEntry>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(rawTitle: String, body: String, existing: JournalEntry?) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val title = derivedTitle(rawTitle, body)
        if (existing == null) {
            repo.insert(
                JournalEntry(
                    title = title,
                    body = body.trim(),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        } else {
            repo.update(
                existing.copy(
                    title = title,
                    body = body.trim(),
                    updatedAtEpochMillis = now,
                ),
            )
        }
    }

    fun delete(entry: JournalEntry) = viewModelScope.launch { repo.delete(entry) }
}
