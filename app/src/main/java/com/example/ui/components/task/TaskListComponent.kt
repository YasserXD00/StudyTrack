package com.example.ui.components.task

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.LanguageSelectorChip
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.StudyTrackViewModel
import com.example.ui.viewmodel.TaskSortOrder
import com.example.ui.viewmodel.TaskStatusFilter
import com.example.ui.viewmodel.TaskViewModel

/**
 * Task List UI Component that displays tasks directly from the Room database
 * and allows users to toggle completion status, filter, sort, search, add, edit, and delete tasks.
 * Fully localized with Arabic, French, Russian, German, Chinese, Japanese, and English support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListComponent(
    viewModel: StudyTrackViewModel,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    showFilters: Boolean = true,
    showSearchBar: Boolean = true,
    onLanguageChangeRequested: ((AppLanguage) -> Unit)? = null
) {
    val tasks by viewModel.tasks.collectAsState()
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current

    var statusFilter by remember { mutableStateOf("All") } // "All", "Pending", "Completed"
    var selectedCategory by remember { mutableStateOf<TaskCategory?>(null) }
    var selectedPriority by remember { mutableStateOf<TaskPriority?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("dueDate") } // "dueDate", "priority", "newest"

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Filter and sort the tasks from Room
    val filteredTasks = remember(tasks, statusFilter, selectedCategory, selectedPriority, searchQuery, sortBy) {
        tasks.filter { task ->
            val matchesStatus = when (statusFilter) {
                "Pending" -> !task.isCompleted
                "Completed" -> task.isCompleted
                else -> true
            }
            val matchesCategory = selectedCategory == null || task.category == selectedCategory
            val matchesPriority = selectedPriority == null || task.priority == selectedPriority
            val matchesSearch = searchQuery.isBlank() ||
                    task.title.contains(searchQuery, ignoreCase = true) ||
                    task.description.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesCategory && matchesPriority && matchesSearch
        }.sortedWith { a, b ->
            when (sortBy) {
                "priority" -> {
                    val orderA = when (a.priority) { TaskPriority.URGENT -> 1; TaskPriority.IMPORTANT -> 2; TaskPriority.NORMAL -> 3 }
                    val orderB = when (b.priority) { TaskPriority.URGENT -> 1; TaskPriority.IMPORTANT -> 2; TaskPriority.NORMAL -> 3 }
                    orderA.compareTo(orderB)
                }
                "newest" -> b.createdAt.compareTo(a.createdAt)
                else -> a.dueDate.compareTo(b.dueDate)
            }
        }
    }

    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val pendingCount = totalCount - completedCount
    val progressRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_list_component"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card with Summary & Progress
        if (showHeader) {
            StudyTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_list_header_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.tasksTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$pendingCount ${strings.pending.lowercase()} • $completedCount ${strings.completed.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Language Switcher Chip
                        LanguageSelectorChip(
                            onOpenDialog = { showLanguageDialog = true }
                        )
                    }

                    // Progress Bar
                    if (totalCount > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.completionRate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progressRatio * 100).toInt()}% ($completedCount/$totalCount)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (progressRatio == 1f) EmeraldSuccess else IndigoPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progressRatio == 1f) EmeraldSuccess else IndigoPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Search and Quick Add Bar
        if (showSearchBar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchTasks) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_search_input")
                )

                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_add_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addTask)
                }
            }
        }

        // 3. Status Filter Tabs (All / Pending / Completed)
        if (showFilters) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val statusOptions = listOf(
                        "All" to "${strings.all} ($totalCount)",
                        "Pending" to "${strings.pending} ($pendingCount)",
                        "Completed" to "${strings.completed} ($completedCount)"
                    )

                    statusOptions.forEach { (key, label) ->
                        val isSelected = statusFilter == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { statusFilter = key }
                                .testTag("task_filter_tab_${key.lowercase()}"),
                            color = if (isSelected) IndigoPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Category & Priority Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Categories chip
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(strings.allCategories) }
                    )
                }

                // Category chips
                items(TaskCategory.values()) { cat ->
                    val isSelected = selectedCategory == cat
                    val catLabel = when (cat) {
                        TaskCategory.STUDIES -> strings.categoryStudies
                        TaskCategory.WORK -> strings.categoryWork
                        TaskCategory.PERSONAL -> strings.categoryPersonal
                        TaskCategory.GOALS -> strings.categoryGoals
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (isSelected) null else cat },
                        label = { Text("${cat.emoji} $catLabel") }
                    )
                }

                // Priority chips
                items(TaskPriority.values()) { prio ->
                    val isSelected = selectedPriority == prio
                    val prioLabel = when (prio) {
                        TaskPriority.URGENT -> strings.urgent
                        TaskPriority.IMPORTANT -> strings.important
                        TaskPriority.NORMAL -> strings.normal
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPriority = if (isSelected) null else prio },
                        label = { Text("${prio.emoji} $prioLabel") }
                    )
                }

                // Reset Filters Button
                if (statusFilter != "All" || selectedCategory != null || selectedPriority != null || searchQuery.isNotBlank()) {
                    item {
                        FilledTonalButton(
                            onClick = {
                                statusFilter = "All"
                                selectedCategory = null
                                selectedPriority = null
                                searchQuery = ""
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.resetFilters, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 4. Task List Items / Empty State
        if (filteredTasks.isEmpty()) {
            StudyTrackCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (tasks.isEmpty()) "🎯" else "🔍", fontSize = 44.sp)
                        Text(
                            text = if (tasks.isEmpty()) strings.noTasksYet else strings.noTasksFound,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (tasks.isEmpty()) strings.noTasksYetDesc else strings.noTasksFoundDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (tasks.isEmpty()) {
                                    showAddDialog = true
                                } else {
                                    statusFilter = "All"
                                    selectedCategory = null
                                    selectedPriority = null
                                    searchQuery = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (tasks.isEmpty()) strings.createFirstTask else strings.resetFilters)
                        }
                    }
                }
            }
        } else {
            // Display each task item card
            filteredTasks.forEach { task ->
                TaskItemCard(
                    task = task,
                    onToggleCompletion = {
                        viewModel.toggleTask(task)
                    },
                    onEdit = { taskToEdit = task },
                    onDelete = { taskToDelete = task }
                )
            }
        }
    }

    // Add Task Dialog
    if (showAddDialog) {
        TaskFormDialog(
            task = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, dueDate, priority, category ->
                viewModel.addTask(title, desc, dueDate, priority, category)
                showAddDialog = false
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        TaskFormDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, desc, dueDate, priority, category ->
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        description = desc,
                        dueDate = dueDate,
                        priority = priority,
                        category = category
                    )
                )
                taskToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmDesc) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentLang,
            onLanguageSelected = { lang ->
                onLanguageChangeRequested?.invoke(lang) ?: viewModel.setLanguage(lang)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * Task List UI Component powered directly by [TaskViewModel] for all business logic
 * including Room database querying, task insertion, status updating, filtering, and sorting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListComponent(
    taskViewModel: TaskViewModel,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    showFilters: Boolean = true,
    showSearchBar: Boolean = true,
    onLanguageChangeRequested: ((AppLanguage) -> Unit)? = null
) {
    val uiState by taskViewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val totalCount = uiState.totalCount
    val completedCount = uiState.completedCount
    val pendingCount = uiState.pendingCount
    val progressRatio = uiState.progressRatio
    val filteredTasks = uiState.filteredTasks

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_list_component"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card with Summary & Progress
        if (showHeader) {
            StudyTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_list_header_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.tasksTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$pendingCount ${strings.pending.lowercase()} • $completedCount ${strings.completed.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Language Switcher Chip
                        LanguageSelectorChip(
                            onOpenDialog = { showLanguageDialog = true }
                        )
                    }

                    // Progress Bar
                    if (totalCount > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.completionRate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progressRatio * 100).toInt()}% ($completedCount/$totalCount)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (progressRatio == 1f) EmeraldSuccess else IndigoPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progressRatio == 1f) EmeraldSuccess else IndigoPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Search and Quick Add Bar
        if (showSearchBar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { taskViewModel.setSearchQuery(it) },
                    placeholder = { Text(strings.searchTasks) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { taskViewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_search_input")
                )

                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_add_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addTask)
                }
            }
        }

        // 3. Status Filter Tabs (All / Pending / Completed)
        if (showFilters) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val statusOptions = listOf(
                        TaskStatusFilter.ALL to "${strings.all} ($totalCount)",
                        TaskStatusFilter.PENDING to "${strings.pending} ($pendingCount)",
                        TaskStatusFilter.COMPLETED to "${strings.completed} ($completedCount)"
                    )

                    statusOptions.forEach { (filter, label) ->
                        val isSelected = uiState.statusFilter == filter
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { taskViewModel.setStatusFilter(filter) }
                                .testTag("task_filter_tab_${filter.name.lowercase()}"),
                            color = if (isSelected) IndigoPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Category & Priority Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Categories chip
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { taskViewModel.setCategoryFilter(null) },
                        label = { Text(strings.allCategories) }
                    )
                }

                // Category chips
                items(TaskCategory.values()) { cat ->
                    val isSelected = uiState.selectedCategory == cat
                    val catLabel = when (cat) {
                        TaskCategory.STUDIES -> strings.categoryStudies
                        TaskCategory.WORK -> strings.categoryWork
                        TaskCategory.PERSONAL -> strings.categoryPersonal
                        TaskCategory.GOALS -> strings.categoryGoals
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { taskViewModel.setCategoryFilter(if (isSelected) null else cat) },
                        label = { Text("${cat.emoji} $catLabel") }
                    )
                }

                // Priority chips
                items(TaskPriority.values()) { prio ->
                    val isSelected = uiState.selectedPriority == prio
                    val prioLabel = when (prio) {
                        TaskPriority.URGENT -> strings.urgent
                        TaskPriority.IMPORTANT -> strings.important
                        TaskPriority.NORMAL -> strings.normal
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { taskViewModel.setPriorityFilter(if (isSelected) null else prio) },
                        label = { Text("${prio.emoji} $prioLabel") }
                    )
                }

                // Reset Filters Button
                if (uiState.statusFilter != TaskStatusFilter.ALL || uiState.selectedCategory != null || uiState.selectedPriority != null || uiState.searchQuery.isNotBlank()) {
                    item {
                        FilledTonalButton(
                            onClick = { taskViewModel.resetFilters() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.resetFilters, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 4. Task List Items / Empty State
        if (filteredTasks.isEmpty()) {
            StudyTrackCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (totalCount == 0) "🎯" else "🔍", fontSize = 44.sp)
                        Text(
                            text = if (totalCount == 0) strings.noTasksYet else strings.noTasksFound,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalCount == 0) strings.noTasksYetDesc else strings.noTasksFoundDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (totalCount == 0) {
                                    showAddDialog = true
                                } else {
                                    taskViewModel.resetFilters()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (totalCount == 0) strings.createFirstTask else strings.resetFilters)
                        }
                    }
                }
            }
        } else {
            // Display each task item card with Room toggle, edit, and delete
            filteredTasks.forEach { task ->
                TaskItemCard(
                    task = task,
                    onToggleCompletion = {
                        taskViewModel.toggleTaskCompletion(task)
                    },
                    onEdit = { taskToEdit = task },
                    onDelete = { taskToDelete = task }
                )
            }
        }
    }

    // Add Task Dialog
    if (showAddDialog) {
        TaskFormDialog(
            task = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, dueDate, priority, category ->
                taskViewModel.addTask(title, desc, dueDate, priority, category)
                showAddDialog = false
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        TaskFormDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, desc, dueDate, priority, category ->
                taskViewModel.updateTask(
                    task.copy(
                        title = title,
                        description = desc,
                        dueDate = dueDate,
                        priority = priority,
                        category = category
                    )
                )
                taskToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmDesc) },
            confirmButton = {
                Button(
                    onClick = {
                        taskViewModel.deleteTask(task)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentLang,
            onLanguageSelected = { lang ->
                taskViewModel.setLanguage(lang)
                onLanguageChangeRequested?.invoke(lang)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * Dialog for adding or editing a task with localization support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormDialog(
    task: TaskItem?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, dueDate: Long, priority: TaskPriority, category: TaskCategory) -> Unit
) {
    val strings = LocalAppStrings.current

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedPriority by remember { mutableStateOf(task?.priority ?: TaskPriority.NORMAL) }
    var selectedCategory by remember { mutableStateOf(task?.category ?: TaskCategory.STUDIES) }
    var dueDateOffsetDays by remember { mutableStateOf(0) }

    val dueDate = remember(dueDateOffsetDays, task?.dueDate) {
        if (task != null && dueDateOffsetDays == 0) {
            task.dueDate
        } else {
            System.currentTimeMillis() + (dueDateOffsetDays * 86400000L)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (task == null) strings.addTask else strings.editTask,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_form_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.taskTitle) },
                    placeholder = { Text(strings.taskTitlePlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_form_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.taskDescription) },
                    placeholder = { Text(strings.taskDescPlaceholder) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Selection
                Text(
                    text = strings.priority,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.values().forEach { prio ->
                        val prioLabel = when (prio) {
                            TaskPriority.URGENT -> strings.urgent
                            TaskPriority.IMPORTANT -> strings.important
                            TaskPriority.NORMAL -> strings.normal
                        }
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text("${prio.emoji} $prioLabel") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Category Selection
                Text(
                    text = strings.category,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskCategory.values().forEach { cat ->
                        val catLabel = when (cat) {
                            TaskCategory.STUDIES -> strings.categoryStudies
                            TaskCategory.WORK -> strings.categoryWork
                            TaskCategory.PERSONAL -> strings.categoryPersonal
                            TaskCategory.GOALS -> strings.categoryGoals
                        }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.emoji} $catLabel", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Due Date Quick Selection
                Text(
                    text = strings.dueDate,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        0 to strings.dueToday,
                        1 to strings.dueTomorrow,
                        3 to "+3d",
                        7 to "+7d"
                    ).forEach { (offset, label) ->
                        FilterChip(
                            selected = dueDateOffsetDays == offset,
                            onClick = { dueDateOffsetDays = offset },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), dueDate, selectedPriority, selectedCategory)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("task_form_confirm_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
