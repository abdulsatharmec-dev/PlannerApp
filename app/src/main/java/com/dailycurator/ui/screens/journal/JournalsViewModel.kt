package com.dailycurator.ui.screens.journal

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.JournalEntry
import com.dailycurator.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class JournalListRow {
    data class SectionHeader(val title: String) : JournalListRow()
    data class EntryRow(val entry: JournalEntry) : JournalListRow()
}

data class JournalListVoicePlayback(val entryId: Long?, val playing: Boolean)

@HiltViewModel
class JournalsViewModel @Inject constructor(
    private val repo: JournalRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dayHeaderFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, MMM d")

    private val _filterDate = MutableStateFlow<LocalDate?>(null)
    val filterDate: StateFlow<LocalDate?> = _filterDate.asStateFlow()

    private val _evergreenOnly = MutableStateFlow(false)
    val evergreenOnly: StateFlow<Boolean> = _evergreenOnly.asStateFlow()

    private var listVoicePlayer: MediaPlayer? = null
    private val _listVoicePlayback = MutableStateFlow(JournalListVoicePlayback(null, false))
    val listVoicePlayback: StateFlow<JournalListVoicePlayback> = _listVoicePlayback.asStateFlow()

    val listRows: StateFlow<List<JournalListRow>> = combine(
        repo.observeAll(),
        _filterDate,
        _evergreenOnly,
    ) { all, filterDay, evOnly ->
        buildList {
            fun entryDay(e: JournalEntry): LocalDate =
                Instant.ofEpochMilli(e.updatedAtEpochMillis).atZone(zone).toLocalDate()

            when {
                evOnly -> {
                    all.filter { it.isEvergreen }
                        .sortedByDescending { it.updatedAtEpochMillis }
                        .forEach { add(JournalListRow.EntryRow(it)) }
                }
                filterDay != null -> {
                    all.filter { entryDay(it) == filterDay }
                        .sortedByDescending { it.updatedAtEpochMillis }
                        .forEach { add(JournalListRow.EntryRow(it)) }
                }
                else -> {
                    val evergreen = all.filter { it.isEvergreen }
                        .sortedByDescending { it.updatedAtEpochMillis }
                    if (evergreen.isNotEmpty()) {
                        add(JournalListRow.SectionHeader("Every day"))
                        evergreen.forEach { add(JournalListRow.EntryRow(it)) }
                    }
                    val byDay = all
                        .filter { !it.isEvergreen }
                        .groupBy { entryDay(it) }
                        .toList()
                        .sortedByDescending { it.first }
                    for ((day, entriesOnDay) in byDay) {
                        add(JournalListRow.SectionHeader(day.format(dayHeaderFmt)))
                        entriesOnDay
                            .sortedByDescending { it.updatedAtEpochMillis }
                            .forEach { add(JournalListRow.EntryRow(it)) }
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterDate(date: LocalDate?) {
        _filterDate.value = date
        if (date != null) _evergreenOnly.value = false
    }

    fun setEvergreenOnly(value: Boolean) {
        _evergreenOnly.value = value
        if (value) _filterDate.value = null
    }

    fun clearFilters() {
        _filterDate.value = null
        _evergreenOnly.value = false
    }

    private fun stopListVoicePlaybackInternal() {
        listVoicePlayer?.release()
        listVoicePlayer = null
        _listVoicePlayback.value = JournalListVoicePlayback(null, false)
    }

    fun toggleListVoicePlayback(entry: JournalEntry) {
        val rel = entry.voiceRelativePath?.trim().orEmpty()
        if (rel.isEmpty()) return
        val file = JournalVoiceFiles.absoluteFile(appContext, rel)
        if (!file.exists()) return

        val state = _listVoicePlayback.value
        if (state.entryId == entry.id) {
            val mp = listVoicePlayer
            if (mp == null) return
            if (state.playing) {
                mp.pause()
                _listVoicePlayback.value = JournalListVoicePlayback(entry.id, false)
            } else {
                mp.start()
                _listVoicePlayback.value = JournalListVoicePlayback(entry.id, true)
            }
            return
        }

        stopListVoicePlaybackInternal()
        runCatching {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.setOnCompletionListener { completed ->
                _listVoicePlayback.value = JournalListVoicePlayback(null, false)
                try {
                    completed.release()
                } catch (_: Throwable) {
                }
                if (listVoicePlayer === completed) listVoicePlayer = null
            }
            mp.start()
            listVoicePlayer = mp
            _listVoicePlayback.value = JournalListVoicePlayback(entry.id, true)
        }.onFailure {
            stopListVoicePlaybackInternal()
        }
    }

    override fun onCleared() {
        stopListVoicePlaybackInternal()
        super.onCleared()
    }

    fun toggleEvergreen(entry: JournalEntry) = viewModelScope.launch {
        repo.update(entry.copy(isEvergreen = !entry.isEvergreen))
    }

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

    fun delete(entry: JournalEntry) = viewModelScope.launch {
        if (_listVoicePlayback.value.entryId == entry.id) {
            stopListVoicePlaybackInternal()
        }
        JournalVoiceFiles.deleteIfExists(appContext, entry.voiceRelativePath)
        repo.delete(entry)
    }
}
