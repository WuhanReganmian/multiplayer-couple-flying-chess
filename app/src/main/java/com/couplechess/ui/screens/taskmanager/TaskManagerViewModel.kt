package com.couplechess.ui.screens.taskmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.couplechess.data.model.Task
import com.couplechess.data.model.TaskLevel
import com.couplechess.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ===== UI State =====

data class TaskManagerUiState(
    val tasks: List<Task> = emptyList(),
    val selectedLevel: TaskLevel? = null,  // null = show all
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val dialogState: TaskDialogState = TaskDialogState.Hidden
)

sealed class TaskDialogState {
    data object Hidden : TaskDialogState()
    data class Add(val form: TaskFormState = TaskFormState()) : TaskDialogState()
    data class Edit(val taskId: String, val form: TaskFormState) : TaskDialogState()
    data class ConfirmDelete(val task: Task) : TaskDialogState()
}

data class TaskFormState(
    val description: String = "",
    val level: TaskLevel = TaskLevel.L1,
    val isValid: Boolean = false,
    val errorMessage: String? = null
)

// ===== ViewModel =====

class TaskManagerViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _selectedLevel = MutableStateFlow<TaskLevel?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _dialogState = MutableStateFlow<TaskDialogState>(TaskDialogState.Hidden)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<TaskManagerUiState> = combine(
        repository.observeAll(),
        _selectedLevel,
        _searchQuery,
        _dialogState,
        _isLoading
    ) { allTasks, selectedLevel, searchQuery, dialogState, isLoading ->
        val filteredTasks = allTasks
            .filter { task ->
                // Filter by level
                selectedLevel == null || task.level == selectedLevel
            }
            .filter { task ->
                // Filter by search query
                searchQuery.isBlank() || task.description.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(compareBy({ it.level.levelInt }, { !it.isCustom }, { it.description }))

        TaskManagerUiState(
            tasks = filteredTasks,
            selectedLevel = selectedLevel,
            searchQuery = searchQuery,
            isLoading = isLoading,
            dialogState = dialogState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskManagerUiState()
    )

    init {
        viewModelScope.launch {
            // Mark loading complete after first emission
            repository.observeAll().collect {
                _isLoading.value = false
            }
        }
    }

    // ===== Filter Actions =====

    fun selectLevel(level: TaskLevel?) {
        _selectedLevel.value = level
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFilters() {
        _selectedLevel.value = null
        _searchQuery.value = ""
    }

    // ===== Dialog Actions =====

    fun showAddDialog() {
        _dialogState.value = TaskDialogState.Add()
    }

    fun showEditDialog(task: Task) {
        _dialogState.value = TaskDialogState.Edit(
            taskId = task.id,
            form = TaskFormState(
                description = task.description,
                level = task.level,
                isValid = task.description.isNotBlank()
            )
        )
    }

    fun showDeleteConfirmation(task: Task) {
        _dialogState.value = TaskDialogState.ConfirmDelete(task)
    }

    fun dismissDialog() {
        _dialogState.value = TaskDialogState.Hidden
    }

    // ===== Form Updates =====

    fun updateFormDescription(description: String) {
        _dialogState.update { currentState ->
            when (currentState) {
                is TaskDialogState.Add -> currentState.copy(
                    form = currentState.form.copy(
                        description = description,
                        isValid = validateForm(description),
                        errorMessage = null
                    )
                )
                is TaskDialogState.Edit -> currentState.copy(
                    form = currentState.form.copy(
                        description = description,
                        isValid = validateForm(description),
                        errorMessage = null
                    )
                )
                else -> currentState
            }
        }
    }

    fun updateFormLevel(level: TaskLevel) {
        _dialogState.update { currentState ->
            when (currentState) {
                is TaskDialogState.Add -> currentState.copy(
                    form = currentState.form.copy(level = level)
                )
                is TaskDialogState.Edit -> currentState.copy(
                    form = currentState.form.copy(level = level)
                )
                else -> currentState
            }
        }
    }

    private fun validateForm(description: String): Boolean {
        return description.trim().length >= 2
    }

    // ===== CRUD Actions =====

    fun saveTask() {
        viewModelScope.launch {
            when (val state = _dialogState.value) {
                is TaskDialogState.Add -> {
                    if (!state.form.isValid) {
                        _dialogState.value = state.copy(
                            form = state.form.copy(errorMessage = "任务描述至少需要2个字符")
                        )
                        return@launch
                    }
                    val newTask = Task(
                        id = UUID.randomUUID().toString(),
                        level = state.form.level,
                        description = state.form.description.trim(),
                        isCustom = true
                    )
                    repository.addCustomTask(newTask)
                    dismissDialog()
                }
                is TaskDialogState.Edit -> {
                    if (!state.form.isValid) {
                        _dialogState.value = state.copy(
                            form = state.form.copy(errorMessage = "任务描述至少需要2个字符")
                        )
                        return@launch
                    }
                    val updatedTask = Task(
                        id = state.taskId,
                        level = state.form.level,
                        description = state.form.description.trim(),
                        isCustom = true  // Editing makes it custom
                    )
                    repository.updateTask(updatedTask)
                    dismissDialog()
                }
                else -> {}
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            dismissDialog()
        }
    }

    // ===== Statistics =====

    fun getTaskCountByLevel(): Map<TaskLevel, Int> {
        val tasks = uiState.value.tasks
        return TaskLevel.entries.associateWith { level ->
            tasks.count { it.level == level }
        }
    }

    // ===== Factory =====

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskManagerViewModel::class.java)) {
                return TaskManagerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
