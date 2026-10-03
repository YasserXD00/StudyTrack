package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.task.TaskListComponent
import com.example.ui.viewmodel.StudyTrackViewModel
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun TasksScreen(
    viewModel: StudyTrackViewModel? = null,
    taskViewModel: TaskViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("tasks_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 80.dp)
        ) {
            TaskListComponent(
                taskViewModel = taskViewModel,
                showHeader = true,
                showFilters = true,
                showSearchBar = true,
                onLanguageChangeRequested = { lang ->
                    viewModel?.setLanguage(lang)
                }
            )
        }
    }
}
