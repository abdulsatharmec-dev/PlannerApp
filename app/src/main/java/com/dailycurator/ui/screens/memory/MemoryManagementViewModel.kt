package com.dailycurator.ui.screens.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.entity.AgentMemoryEntity
import com.dailycurator.data.repository.AgentMemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryManagementViewModel @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
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
}
