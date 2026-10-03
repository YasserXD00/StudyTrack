package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.pomodoro.PomodoroStudyTimerComponent
import com.example.ui.components.pomodoro.PomodoroTimerViewModel
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.StudyTrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: StudyTrackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    timerViewModel: PomodoroTimerViewModel = viewModel()
) {
    val strings = LocalAppStrings.current
    val sessionProgress by timerViewModel.sessionProgress.collectAsState()

    // Listen to session events from timerViewModel and record in StudyTrackViewModel
    LaunchedEffect(timerViewModel) {
        timerViewModel.onFocusSessionCompleted = { duration, subject ->
            viewModel.recordPomodoroCompletion(duration, subject)
        }
    }

    Scaffold(
        modifier = modifier.testTag("pomodoro_screen"),
        topBar = {
            TopAppBar(
                title = { Text("${strings.pomodoroTitle} ⏱️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.close)
                    }
                },
                actions = {
                    Surface(
                        color = AmberAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🍅 ${strings.recentSessions}: ${sessionProgress.completedSessionsToday}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AmberAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Pomodoro-style study timer component
            PomodoroStudyTimerComponent(
                timerViewModel = timerViewModel,
                onSessionCompletedCallback = { duration, subject ->
                    viewModel.recordPomodoroCompletion(duration, subject)
                }
            )

            // Educational / Motivation Card
            StudyTrackCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "The Pomodoro Technique Guide",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = IndigoPrimary
                        )
                    }
                    Text(
                        text = "1. Pick a task & eliminate all distractions.\n" +
                                "2. Focus uninterrupted for 25 minutes.\n" +
                                "3. Take a 5-minute break to recharge.\n" +
                                "4. Complete 4 pomodoros, then enjoy a well-deserved 15-minute long break.\n" +
                                "⭐ Each completed focus session awards +20 XP to your profile!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
