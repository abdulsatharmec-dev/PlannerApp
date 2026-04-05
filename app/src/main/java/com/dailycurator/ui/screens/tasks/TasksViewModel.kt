package com.dailycurator.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<PriorityTask> = emptyList(),
    val listDate: LocalDate = LocalDate.now()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val listDate = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.launch {
            listDate.flatMapLatest { d ->
                repo.getTasksForDate(d).map { tasks -> d to tasks }
            }.collect { (d, tasks) ->
                _uiState.update { it.copy(tasks = tasks, listDate = d) }
            }
        }
    }

    fun setListDate(date: LocalDate) {
        listDate.value = date
    }

    fun addTask(
        title: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency,
        isTop5: Boolean,
        note: String?
    ) = viewModelScope.launch {
        val nextRank = if (isTop5) 1 else ((_uiState.value.tasks.maxOfOrNull { it.rank } ?: 0) + 1)
        repo.insert(
            PriorityTask(
                rank = nextRank, title = title,
                startTime = startTime, endTime = endTime, statusNote = note,
                urgency = urgency, date = date
            )
        )
    }

    fun updateTask(
        task: PriorityTask,
        title: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency,
        isTop5: Boolean,
        note: String?
    ) = viewModelScope.launch {
        val updatedRank = if (isTop5) 1 else task.rank.coerceAtLeast(2)
        repo.update(task.copy(
            title = title, date = date, startTime = startTime, endTime = endTime,
            urgency = urgency, rank = updatedRank, statusNote = note
        ))
    }

    fun deleteTask(task: PriorityTask) = viewModelScope.launch {
        repo.delete(task)
    }

    fun toggleTaskDone(task: PriorityTask) = viewModelScope.launch {
        repo.toggleDone(task)
    }
}
