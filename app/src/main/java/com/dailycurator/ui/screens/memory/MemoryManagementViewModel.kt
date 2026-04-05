package com.dailycurator.ui.screens.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.entity.AgentMemoryEntity
import com.dailycurator.data.repository.AgentMemoryRepository
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.HabitRepository
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MemoryManagementViewModel @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    val entries: StateFlow<List<AgentMemoryEntity>> = agentMemoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addMemory(text: String) = viewModelScope.launch {
        agentMemoryRepository.insertManual(text)
    }

    fun delete(entity: AgentMemoryEntity) = viewModelScope.launch {
        agentMemoryRepository.delete(entity)
    }

    fun saveEdit(entity: AgentMemoryEntity, newContent: String) = viewModelScope.launch {
        agentMemoryRepository.saveUserEdit(entity, newContent)
    }

    fun ingestFromPlanner() = viewModelScope.launch {
        if (prefs.getCerebrasKey().isBlank()) return@launch
        if (!prefs.isAgentMemoryEnabled()) return@launch
        val snap = buildPlannerSnapshot()
        agentMemoryRepository.ingestFromPlannerContext(snap)
    }

    private suspend fun buildPlannerSnapshot(): String {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val tasks = taskRepository.getTasksForDate(today).firstOrNull() ?: emptyList()
        val habits = habitRepository.getHabitsForDate(today).firstOrNull() ?: emptyList()
        val goals = goalRepository.getGoalsForWeek(weekStart).firstOrNull() ?: emptyList()
        val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return buildString {
            appendLine("Current Time: ${now.format(timeFmt)}")
            appendLine("--- TASKS FOR TODAY ---")
            if (tasks.isEmpty()) appendLine("None")
            tasks.forEach {
                appendLine("- id=${it.id} [${if (it.isDone) "X" else " "}] ${it.title} (${it.startTime}-${it.endTime}) urgency=${it.urgency}")
            }
            appendLine("\n--- HABITS FOR TODAY ---")
            if (habits.isEmpty()) appendLine("None")
            habits.forEach {
                appendLine("- id=${it.id} [${if (it.isDone) "X" else " "}] ${it.name} (${it.currentValue}/${it.targetValue} ${it.unit})")
            }
            appendLine("\n--- GOALS FOR THIS WEEK ---")
            if (goals.isEmpty()) appendLine("None")
            goals.forEach {
                appendLine("- id=${it.id} [${if (it.isCompleted) "X" else " "}] ${it.title}: ${it.description} deadline=${it.deadline}")
            }
        }
    }
}
