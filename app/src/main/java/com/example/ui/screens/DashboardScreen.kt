package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.ui.components.*
import com.example.ui.components.task.TaskItemCard
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudyTrackViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: StudyTrackViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToPomodoro: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToBooks: () -> Unit,
    onNavigateToEnglish: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onOpenAddTaskDialog: () -> Unit,
    onOpenAddExpenseDialog: () -> Unit,
    onOpenLogStudyDialog: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val habitLogs by viewModel.habitLogs.collectAsState()
    val metrics by viewModel.productivityMetrics.collectAsState()
    val currentLang = LocalAppLanguage.current
    val strings = LocalAppStrings.current

    // Date and Greeting calculations
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greetingTime = when (hour) {
        in 5..11 -> strings.goodMorning
        in 12..17 -> strings.goodAfternoon
        else -> strings.goodEvening
    }
    val dateFormat = remember(currentLang) { SimpleDateFormat("EEEE, d MMMM", Locale(currentLang.code)) }
    val todayDateStr = remember(currentLang, dateFormat) { dateFormat.format(Date()) }
    val ymdFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayKey = remember { ymdFormat.format(Date()) }

    // Today's Study Time
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayStudyMinutes = sessions.filter { it.dateTimestamp >= todayStart }.sumOf { it.durationMinutes }
    val studyHours = todayStudyMinutes / 60
    val studyMinsRem = todayStudyMinutes % 60
    val studyTimeDisplay = if (studyHours > 0) "${studyHours}h ${studyMinsRem}m" else "${studyMinsRem}m"

    val completedTasksCount = tasks.count { it.isCompleted }
    val totalTasksCount = tasks.size
    val tasksDisplay = "$completedTasksCount/$totalTasksCount"

    var showLanguageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Personalized Header & Level Banner
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$greetingTime, ${profile.name} 👋",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = todayDateStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageSelectorChip(onOpenDialog = { showLanguageDialog = true })

                        // Level Badge
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToAchievements() },
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🏆", fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = "${strings.level} ${profile.level}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${profile.currentLevelXp}/100 ${strings.xp}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // XP Progress bar
                LinearProgressIndicator(
                    progress = { profile.currentLevelXp / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AmberAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // 2. Today's Productivity Score Card
        item {
            StudyTrackCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⭐ ${strings.todayProductivity}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                metrics.score >= 80 -> "Outstanding momentum! You're crushing today's study and habits."
                                metrics.score >= 60 -> "Great progress! Keep up the focused effort."
                                else -> "Ready to kickstart your study and finish tasks?"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // Breakdown tags
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${strings.navTasks}: ${metrics.taskPart}/30",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = IndigoLight
                            )
                            Text(
                                text = "${strings.navStudy}: ${metrics.studyPart}/30",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = CyanAccent
                            )
                            Text(
                                text = "${strings.navHabits}: ${metrics.habitPart}/20",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = EmeraldSuccess
                            )
                        }
                    }

                    ProductivityGauge(score = metrics.score, size = 94.dp)
                }
            }
        }

        // 3. 4 Key Stat Badges (Study, Tasks, Streak, Productivity)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    icon = "⏱️",
                    label = strings.studyToday,
                    value = studyTimeDisplay,
                    accentColor = IndigoLight,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToStudy
                )
                StatCard(
                    icon = "📝",
                    label = strings.tasksDone,
                    value = tasksDisplay,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    icon = "🔥",
                    label = strings.currentStreak,
                    value = "${profile.streakDays} ${strings.days}",
                    accentColor = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "🎯",
                    label = strings.goalsActive,
                    value = "${goals.size} ${strings.active}",
                    accentColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToGoals
                )
            }
        }

        // 4. Quick Actions
        item {
            SectionHeader(title = strings.quickActions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip(
                    icon = Icons.Default.Add,
                    label = strings.newTask,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAddTaskDialog
                )
                ActionChip(
                    icon = Icons.Default.Timer,
                    label = strings.focusTimer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPomodoro
                )
                ActionChip(
                    icon = Icons.Default.AttachMoney,
                    label = strings.expense,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAddExpenseDialog
                )
                ActionChip(
                    icon = Icons.Default.Book,
                    label = strings.logStudy,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLogStudyDialog
                )
            }
        }

        // 5. Explore Sections (Horizontal scrollable quick shortcuts)
        item {
            SectionHeader(title = strings.moreModules)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    ModuleShortcutCard(
                        emoji = "⏱️",
                        title = strings.navFocus,
                        subtitle = strings.pomodoroFocus,
                        accent = IndigoLight,
                        onClick = onNavigateToPomodoro
                    )
                }
                item {
                    ModuleShortcutCard(
                        emoji = "💰",
                        title = strings.navExpenses,
                        subtitle = strings.expensesTitle,
                        accent = EmeraldSuccess,
                        onClick = onNavigateToExpenses
                    )
                }
                item {
                    ModuleShortcutCard(
                        emoji = "📖",
                        title = strings.navBooks,
                        subtitle = strings.booksTitle,
                        accent = AmberAccent,
                        onClick = onNavigateToBooks
                    )
                }
                item {
                    ModuleShortcutCard(
                        emoji = "🇬🇧",
                        title = strings.navEnglish,
                        subtitle = strings.englishTitle,
                        accent = CyanAccent,
                        onClick = onNavigateToEnglish
                    )
                }
                item {
                    ModuleShortcutCard(
                        emoji = "🏆",
                        title = strings.navTrophies,
                        subtitle = strings.achievementsTitle,
                        accent = PurpleAccent,
                        onClick = onNavigateToAchievements
                    )
                }
            }
        }

        // 6. Today's Tasks
        item {
            SectionHeader(
                title = strings.taskList,
                actionText = "${strings.all} (${tasks.size})",
                onActionClick = onNavigateToTasks
            )
        }

        val pendingTasks = tasks.filter { !it.isCompleted }
        val displayedTasks = (pendingTasks.take(3) + tasks.filter { it.isCompleted }.take(1))

        if (displayedTasks.isEmpty()) {
            item {
                StudyTrackCard {
                    Text(
                        text = strings.noTasksYetDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedTasks, key = { it.id }) { task ->
                TaskItemCard(
                    task = task,
                    onToggleCompletion = { viewModel.toggleTask(task) },
                    onEdit = onNavigateToTasks,
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
        }

        // 7. Today's Habits
        item {
            SectionHeader(
                title = strings.dailyHabits,
                actionText = strings.navHabits,
                onActionClick = onNavigateToHabits
            )
        }

        if (habits.isEmpty()) {
            item {
                StudyTrackCard {
                    Text(
                        text = strings.noTasksYetDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                StudyTrackCard {
                    habits.forEach { habit ->
                        val isDoneToday = habitLogs.any { it.habitId == habit.id && it.dateString == todayKey }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = habit.emoji, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${strings.currentStreak}: 🔥 ${habit.currentStreak} ${strings.days}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledIconToggleButton(
                                checked = isDoneToday,
                                onCheckedChange = { viewModel.toggleHabitToday(habit.id) },
                                colors = IconButtonDefaults.filledIconToggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    checkedContainerColor = EmeraldSuccess
                                )
                            ) {
                                if (isDoneToday) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Mark done")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        val currentLang = LocalAppLanguage.current
        LanguageSelectorDialog(
            currentLanguage = currentLang,
            onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun TaskItemRow(
    task: TaskItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            PriorityBadge(priority = task.priority.label, emoji = task.priority.emoji)
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ModuleShortcutCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
