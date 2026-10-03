package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import com.example.data.model.HabitItem
import com.example.data.model.HabitLog
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.SectionHeader
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.StudyTrackViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HabitsScreen(
    viewModel: StudyTrackViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val habits by viewModel.habits.collectAsState()
    val logs by viewModel.habitLogs.collectAsState()

    var showAddHabitDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayLabelFormat = remember(currentLang) { SimpleDateFormat("EEE", Locale(currentLang.code)) }
    val todayKey = remember { dateFormat.format(Date()) }

    // Last 7 days list
    val last7Days = remember(currentLang) {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            list.add(Pair(dateFormat.format(cal.time), dayLabelFormat.format(cal.time)))
        }
        list
    }

    val todayCompletedCount = habits.count { h -> logs.any { it.habitId == h.id && it.dateString == todayKey } }
    val completionRatio = if (habits.isNotEmpty()) ((todayCompletedCount.toFloat() / habits.size) * 100).toInt() else 0

    Scaffold(
        modifier = modifier.testTag("habits_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddHabitDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_habit_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addHabit)
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
            // Summary Card
            item {
                StudyTrackCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${strings.habitsTitle} 🌱",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${strings.today}: $todayCompletedCount / ${habits.size} ${strings.completed.lowercase()} ($completionRatio%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$completionRatio% ${strings.completed}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { completionRatio / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldSuccess,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Habits List
            item {
                SectionHeader(title = strings.dailyHabits)
            }

            if (habits.isEmpty()) {
                item {
                    StudyTrackCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
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
                items(habits, key = { it.id }) { habit ->
                    val isDoneToday = logs.any { it.habitId == habit.id && it.dateString == todayKey }
                    HabitCard(
                        habit = habit,
                        isDoneToday = isDoneToday,
                        last7Days = last7Days,
                        logs = logs,
                        onToggleToday = { viewModel.toggleHabitToday(habit.id) },
                        onDelete = { viewModel.deleteHabit(habit) }
                    )
                }
            }
        }
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { title, emoji, category ->
                viewModel.addHabit(title, emoji, category)
                showAddHabitDialog = false
            }
        )
    }
}

@Composable
fun HabitCard(
    habit: HabitItem,
    isDoneToday: Boolean,
    last7Days: List<Pair<String, String>>,
    logs: List<HabitLog>,
    onToggleToday: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Habit Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = habit.emoji, fontSize = 22.sp)
                    }

                    Column {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🔥 ${strings.currentStreak}: ${habit.currentStreak} ${strings.days}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberAccent
                            )
                            Text(
                                text = "🏆 ${habit.bestStreak} ${strings.days}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconToggleButton(
                        checked = isDoneToday,
                        onCheckedChange = { onToggleToday() },
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            checkedContainerColor = EmeraldSuccess
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isDoneToday) {
                            Icon(Icons.Default.Check, contentDescription = strings.completed, tint = Color.White)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = strings.markComplete)
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7 Days Weekly Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                last7Days.forEach { (dateKey, dayLabel) ->
                    val isCompleted = logs.any { it.habitId == habit.id && it.dateString == dateKey }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, emoji: String, category: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🌱") }
    var category by remember { mutableStateOf("General") }

    val sampleEmojis = listOf("📖", "💪", "💧", "🇬🇧", "✍️", "🧘", "🥗", "🎯")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addHabit, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("${strings.taskTitle} *") },
                    placeholder = { Text(strings.taskTitlePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Emoji:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleEmojis.forEach { emoji ->
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { selectedEmoji = emoji },
                            color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(strings.category) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), selectedEmoji, category.trim())
                    }
                },
                enabled = title.isNotBlank()
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
