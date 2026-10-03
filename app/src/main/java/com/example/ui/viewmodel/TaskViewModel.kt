package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.StudyTrackRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getAppStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Filter options for task status.
 */
enum class TaskStatusFilter {
    ALL,
    PENDING,
    COMPLETED
}

/**
 * Sort options for tasks.
 */
enum class TaskSortOrder {
    DUE_DATE,
    PRIORITY,
    NEWEST,
    TITLE
}

/**
 * UI State representing the state of tasks in the application.
 */
data class TaskUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filteredTasks: List<TaskItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val statusFilter: TaskStatusFilter = TaskStatusFilter.ALL,
    val selectedCategory: TaskCategory? = null,
    val selectedPriority: TaskPriority? = null,
    val sortOrder: TaskSortOrder = TaskSortOrder.DUE_DATE,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val progressRatio: Float = 0f
)

/**
 * One-off UI events emitted by [TaskViewModel].
 */
sealed class TaskUiEvent {
    data class TaskAdded(val task: TaskItem, val message: String) : TaskUiEvent()
    data class TaskUpdated(val task: TaskItem, val message: String) : TaskUiEvent()
    data class TaskCompleted(val task: TaskItem, val xpEarned: Int, val message: String) : TaskUiEvent()
    data class TaskDeleted(val task: TaskItem, val message: String) : TaskUiEvent()
    data class ShowMessage(val message: String) : TaskUiEvent()
    data class ShowError(val error: String) : TaskUiEvent()
}

/**
 * ViewModel responsible for the business logic of fetching, adding, updating,
 * and deleting tasks in the Room database, as well as filtering, searching,
 * sorting, and calculating completion metrics.
 */
class TaskViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: StudyTrackRepository = StudyTrackRepository(AppDatabase.getDatabase(application)),
    private val started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : AndroidViewModel(application) {


    // Internal filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(TaskStatusFilter.ALL)
    val statusFilter: StateFlow<TaskStatusFilter> = _statusFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TaskCategory?>(null)
    val selectedCategory: StateFlow<TaskCategory?> = _selectedCategory.asStateFlow()

    private val _selectedPriority = MutableStateFlow<TaskPriority?>(null)
    val selectedPriority: StateFlow<TaskPriority?> = _selectedPriority.asStateFlow()

    private val _sortOrder = MutableStateFlow(TaskSortOrder.DUE_DATE)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Single-event channel for UI notifications
    private val _eventChannel = Channel<TaskUiEvent>(Channel.BUFFERED)
    val events: Flow<TaskUiEvent> = _eventChannel.receiveAsFlow()

    // Active language for localized feedback messages
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Flow of raw tasks fetched from the Room database
    val allTasks: StateFlow<List<TaskItem>> = repository.tasks
        .catch { e ->
            _errorMessage.value = "Failed to load tasks: ${e.message}"
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = started,
            initialValue = emptyList()
        )

    private data class TaskFilterState(
        val query: String = "",
        val status: TaskStatusFilter = TaskStatusFilter.ALL,
        val category: TaskCategory? = null,
        val priority: TaskPriority? = null,
        val sortOrder: TaskSortOrder = TaskSortOrder.DUE_DATE
    )

    private val filterState = combine(
        _searchQuery,
        _statusFilter,
        _selectedCategory,
        _selectedPriority,
        _sortOrder
    ) { query, status, category, priority, sort ->
        TaskFilterState(query, status, category, priority, sort)
    }

    // Combined UI State combining Room tasks with search, filter, and sort business logic
    val uiState: StateFlow<TaskUiState> = combine(
        allTasks,
        filterState,
        _isLoading,
        _errorMessage
    ) { rawTasks, filters, loading, error ->
        val total = rawTasks.size
        val completed = rawTasks.count { it.isCompleted }
        val pending = total - completed
        val ratio = if (total > 0) completed.toFloat() / total else 0f

        // Apply filtering logic
        val filtered = rawTasks.filter { task ->
            val matchesStatus = when (filters.status) {
                TaskStatusFilter.ALL -> true
                TaskStatusFilter.PENDING -> !task.isCompleted
                TaskStatusFilter.COMPLETED -> task.isCompleted
            }
            val matchesCategory = filters.category == null || task.category == filters.category
            val matchesPriority = filters.priority == null || task.priority == filters.priority
            val matchesQuery = filters.query.isBlank() ||
                    task.title.contains(filters.query, ignoreCase = true) ||
                    task.description.contains(filters.query, ignoreCase = true)

            matchesStatus && matchesCategory && matchesPriority && matchesQuery
        }.sortedWith { a, b ->
            when (filters.sortOrder) {
                TaskSortOrder.PRIORITY -> {
                    val orderA = when (a.priority) {
                        TaskPriority.URGENT -> 1
                        TaskPriority.IMPORTANT -> 2
                        TaskPriority.NORMAL -> 3
                    }
                    val orderB = when (b.priority) {
                        TaskPriority.URGENT -> 1
                        TaskPriority.IMPORTANT -> 2
                        TaskPriority.NORMAL -> 3
                    }
                    orderA.compareTo(orderB)
                }
                TaskSortOrder.NEWEST -> b.createdAt.compareTo(a.createdAt)
                TaskSortOrder.TITLE -> a.title.compareTo(b.title, ignoreCase = true)
                TaskSortOrder.DUE_DATE -> a.dueDate.compareTo(b.dueDate)
            }
        }

        TaskUiState(
            tasks = rawTasks,
            filteredTasks = filtered,
            isLoading = loading,
            errorMessage = error,
            searchQuery = filters.query,
            statusFilter = filters.status,
            selectedCategory = filters.category,
            selectedPriority = filters.priority,
            sortOrder = filters.sortOrder,
            totalCount = total,
            pendingCount = pending,
            completedCount = completed,
            progressRatio = ratio
        )
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = TaskUiState()
    )

    init {
        // Observe language from repository user profile continuously
        viewModelScope.launch {
            repository.profile.filterNotNull().collect { profile ->
                val lang = AppLanguage.fromCode(profile.languageCode)
                if (_currentLanguage.value != lang) {
                    _currentLanguage.value = lang
                }
            }
        }
    }

    // =========================================================================
    // Filter & Query Controls
    // =========================================================================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: TaskStatusFilter) {
        _statusFilter.value = filter
    }

    fun setCategoryFilter(category: TaskCategory?) {
        _selectedCategory.value = category
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _selectedPriority.value = priority
    }

    fun setSortOrder(order: TaskSortOrder) {
        _sortOrder.value = order
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _statusFilter.value = TaskStatusFilter.ALL
        _selectedCategory.value = null
        _selectedPriority.value = null
        _sortOrder.value = TaskSortOrder.DUE_DATE
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        viewModelScope.launch {
            val current = repository.profile.firstOrNull() ?: com.example.data.model.UserProfile()
            repository.updateProfile(current.copy(languageCode = language.code))
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // =========================================================================
    // Business Logic: Fetching
    // =========================================================================

    /**
     * Retrieve a specific task by its unique ID as a reactive Flow.
     */
    fun getTaskById(taskId: Long): Flow<TaskItem?> {
        return allTasks.map { list -> list.firstOrNull { it.id == taskId } }
    }

    /**
     * Retrieve a task from the currently held in-memory list synchronously.
     */
    fun findTask(taskId: Long): TaskItem? {
        return allTasks.value.firstOrNull { it.id == taskId }
    }

    // =========================================================================
    // Business Logic: Adding Tasks
    // =========================================================================

    /**
     * Business logic to validate and add a new task to the Room database.
     * Validates that the title is not blank, sanitizes input, and saves to database.
     */
    fun addTask(
        title: String,
        description: String = "",
        dueDate: Long = System.currentTimeMillis(),
        priority: TaskPriority = TaskPriority.NORMAL,
        category: TaskCategory = TaskCategory.STUDIES
    ): Job = viewModelScope.launch {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            val error = "Task title cannot be empty"
            _errorMessage.value = error
            _eventChannel.send(TaskUiEvent.ShowError(error))
            return@launch
        }

        try {
            _isLoading.value = true
            val newTask = TaskItem(
                title = trimmedTitle,
                description = description.trim(),
                dueDate = dueDate,
                priority = priority,
                category = category,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )

            val generatedId = repository.insertTask(newTask)
            val persistedTask = newTask.copy(id = generatedId)

            val strings = getAppStrings(_currentLanguage.value)
            _eventChannel.send(
                TaskUiEvent.TaskAdded(
                    task = persistedTask,
                    message = strings.taskAddedCelebration
                )
            )
        } catch (e: Exception) {
            val error = "Failed to add task: ${e.localizedMessage ?: "Unknown error"}"
            _errorMessage.value = error
            _eventChannel.send(TaskUiEvent.ShowError(error))
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Add an existing [TaskItem] entity directly into the Room database.
     */
    fun addTask(task: TaskItem): Job = addTask(
        title = task.title,
        description = task.description,
        dueDate = task.dueDate,
        priority = task.priority,
        category = task.category
    )

    // =========================================================================
    // Business Logic: Updating Tasks
    // =========================================================================

    /**
     * Update an existing task in the Room database.
     */
    fun updateTask(task: TaskItem): Job = viewModelScope.launch {
        val trimmedTitle = task.title.trim()
        if (trimmedTitle.isBlank()) {
            val error = "Task title cannot be empty"
            _errorMessage.value = error
            _eventChannel.send(TaskUiEvent.ShowError(error))
            return@launch
        }

        try {
            _isLoading.value = true
            val updatedTask = task.copy(title = trimmedTitle, description = task.description.trim())
            repository.updateTask(updatedTask)

            _eventChannel.send(
                TaskUiEvent.TaskUpdated(
                    task = updatedTask,
                    message = "Task updated successfully"
                )
            )
        } catch (e: Exception) {
            val error = "Failed to update task: ${e.localizedMessage ?: "Unknown error"}"
            _errorMessage.value = error
            _eventChannel.send(TaskUiEvent.ShowError(error))
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update individual fields of a task identified by [taskId].
     */
    fun updateTaskDetails(
        taskId: Long,
        title: String,
        description: String,
        dueDate: Long,
        priority: TaskPriority,
        category: TaskCategory
    ): Job = viewModelScope.launch {
        val existing = findTask(taskId)
        if (existing == null) {
            _errorMessage.value = "Task not found"
            return@launch
        }
        updateTask(
            existing.copy(
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority,
                category = category
            )
        )
    }

    /**
     * Toggle the completion status of a task in the Room database.
     * When completed, awards +10 XP and triggers completion celebration.
     */
    fun toggleTaskCompletion(task: TaskItem): Job = viewModelScope.launch {
        try {
            val isNowCompleted = repository.toggleTaskCompleted(task)
            val updatedTask = task.copy(isCompleted = isNowCompleted)

            val strings = getAppStrings(_currentLanguage.value)
            if (isNowCompleted) {
                _eventChannel.send(
                    TaskUiEvent.TaskCompleted(
                        task = updatedTask,
                        xpEarned = 10,
                        message = strings.taskCompletedXp
                    )
                )
            } else {
                _eventChannel.send(
                    TaskUiEvent.ShowMessage(strings.markIncomplete)
                )
            }
        } catch (e: Exception) {
            val error = "Failed to toggle task: ${e.localizedMessage ?: "Unknown error"}"
            _errorMessage.value = error
            _eventChannel.send(TaskUiEvent.ShowError(error))
        }
    }

    /**
     * Toggle completion by task ID.
     */
    fun toggleTaskCompletion(taskId: Long): Job = viewModelScope.launch {
        val existing = findTask(taskId) ?: return@launch
        toggleTaskCompletion(existing)
    }

    // =========================================================================
    // Business Logic: Deleting Tasks
    // =========================================================================

    /**
     * Delete a task from the Room database.
     */
    fun deleteTask(task: TaskItem): Job = viewModelScope.launch {
        try {
            repository.deleteTask(task)
            _eventChannel.send(
                TaskUiEvent.TaskDeleted(task = task, message = "Task deleted")
            )
        } catch (e: Exception) {
            val error = "Failed to delete task: ${e.localizedMessage ?: "Unknown error"}"
            _errorMessage.value = error
            _eventChannel.send(TaskUiEvent.ShowError(error))
        }
    }

    /**
     * Delete a task by ID.
     */
    fun deleteTaskById(taskId: Long): Job = viewModelScope.launch {
        val existing = findTask(taskId)
        if (existing != null) {
            deleteTask(existing)
        } else {
            repository.deleteTaskById(taskId)
        }
    }

    /**
     * Clear all completed tasks from the Room database.
     */
    fun clearCompletedTasks(): Job = viewModelScope.launch {
        try {
            val completed = allTasks.value.filter { it.isCompleted }
            completed.forEach { task ->
                repository.deleteTask(task)
            }
            if (completed.isNotEmpty()) {
                _eventChannel.send(
                    TaskUiEvent.ShowMessage("Cleared ${completed.size} completed tasks")
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to clear completed tasks"
        }
    }
}
