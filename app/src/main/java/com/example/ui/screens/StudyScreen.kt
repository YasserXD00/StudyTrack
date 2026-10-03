package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudySession
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.PurpleAccent
import com.example.ui.viewmodel.StudyTrackViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudyScreen(
    viewModel: StudyTrackViewModel,
    onNavigateToPomodoro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val sessions by viewModel.studySessions.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }

    // Time boundary calculations
    val now = remember { System.currentTimeMillis() }
    val cal = Calendar.getInstance()

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = cal.timeInMillis

    val studyTodayMinutes = sessions.filter { it.dateTimestamp >= todayStart }.sumOf { it.durationMinutes }
    val studyWeekMinutes = sessions.filter { it.dateTimestamp >= weekStart }.sumOf { it.durationMinutes }
    val studyMonthMinutes = sessions.filter { it.dateTimestamp >= monthStart }.sumOf { it.durationMinutes }

    val daysInActiveMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val avgDailyMinutes = studyMonthMinutes / daysInActiveMonth

    // Most studied subject
    val subjectCounts = sessions.groupBy { it.subject }.mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
    val mostStudied = subjectCounts.maxByOrNull { it.value }?.key ?: "None yet"

    // Past 7 days data for SimpleBarChart
    val dayFormat = remember(currentLang) { SimpleDateFormat("EEE", Locale(currentLang.code)) }
    val past7DaysData = remember(sessions, currentLang) {
        val list = mutableListOf<Pair<String, Float>>()
        val dayCal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            dayCal.timeInMillis = System.currentTimeMillis()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            val start = dayCal.timeInMillis
            val end = start + 24 * 3600 * 1000L
            val dayMins = sessions.filter { it.dateTimestamp in start until end }.sumOf { it.durationMinutes }
            list.add(Pair(dayFormat.format(dayCal.time), dayMins / 60f))
        }
        list
    }

    Scaffold(
        modifier = modifier.testTag("study_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("log_study_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.logSession)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Pomodoro Callout
            item {
                StudyTrackCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.studyStats} 📚",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.tasksSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = onNavigateToPomodoro,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.focusTimer)
                        }
                    }
                }
            }

            // Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = "📅",
                        label = strings.today,
                        value = "${studyTodayMinutes / 60}${strings.hours} ${studyTodayMinutes % 60}${strings.minutes}",
                        accentColor = IndigoLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "📊",
                        label = strings.thisWeek,
                        value = "${studyWeekMinutes / 60}${strings.hours} ${studyWeekMinutes % 60}${strings.minutes}",
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = "🗓️",
                        label = strings.thisMonth,
                        value = "${studyMonthMinutes / 60}${strings.hours}",
                        accentColor = PurpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "⭐",
                        label = strings.subject,
                        value = mostStudied.take(14),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Evolution Chart
            item {
                StudyTrackCard {
                    Text(
                        text = "${strings.studyStats} (${strings.hours})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SimpleBarChart(data = past7DaysData, barColor = IndigoLight)
                }
            }

            // Session History Section
            item {
                SectionHeader(title = "${strings.recentSessions} (${sessions.size})")
            }

            if (sessions.isEmpty()) {
                item {
                    StudyTrackCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noTasksYetDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    StudySessionRow(
                        session = session,
                        onDelete = { viewModel.deleteStudySession(session) }
                    )
                }
            }
        }
    }

    if (showLogDialog) {
        LogStudySessionDialog(
            onDismiss = { showLogDialog = false },
            onConfirm = { subject, duration, notes ->
                viewModel.logStudySession(subject, duration, notes)
                showLogDialog = false
            }
        )
    }
}

@Composable
fun StudySessionRow(
    session: StudySession,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val dateFormat = remember(currentLang) { SimpleDateFormat("dd MMM, hh:mm a", Locale(currentLang.code)) }
    val dateDisplay = remember(session.dateTimestamp, dateFormat) { dateFormat.format(Date(session.dateTimestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = session.subject,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (session.isPomodoro) {
                        Surface(
                            color = IndigoLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🍅 ${strings.pomodoroFocus}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = IndigoLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (session.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${session.durationMinutes} ${strings.minutes}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = strings.delete,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun LogStudySessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (subject: String, duration: Int, notes: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var subject by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("45") }
    var notes by remember { mutableStateOf("") }

    val presetSubjects = listOf("Computer Science", "Calculus", "Linear Algebra", "English", "Physics", "Algorithms")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.logSession, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("${strings.subject} *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick subject suggestions
                Text("${strings.subject}:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetSubjects.take(3).forEach { sub ->
                        FilterChip(
                            selected = subject == sub,
                            onClick = { subject = sub },
                            label = { Text(sub, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) durationText = it },
                    label = { Text("${strings.minutes} *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notes) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationText.toIntOrNull() ?: 30
                    if (subject.isNotBlank()) {
                        onConfirm(subject.trim(), duration, notes.trim())
                    }
                },
                enabled = subject.isNotBlank() && durationText.isNotBlank()
            ) {
                Text("${strings.save} (+20 XP)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
