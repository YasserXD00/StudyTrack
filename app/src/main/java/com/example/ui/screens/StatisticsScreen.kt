package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.ProductivityGauge
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudyTrackViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(
    viewModel: StudyTrackViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val logs by viewModel.habitLogs.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(strings.tabStudy, strings.tabTasks, strings.tabExpenses, strings.tabHabits)

    // Calculations
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val todayStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = cal.timeInMillis

    // Study
    val todayStudyMin = sessions.filter { it.dateTimestamp >= todayStart }.sumOf { it.durationMinutes }
    val weekStudyMin = sessions.filter { it.dateTimestamp >= weekStart }.sumOf { it.durationMinutes }
    val monthStudyMin = sessions.filter { it.dateTimestamp >= monthStart }.sumOf { it.durationMinutes }
    val subjectCounts = sessions.groupBy { it.subject }.mapValues { entry -> entry.value.sumOf { it.durationMinutes } }

    val dayFormat = remember(currentLang) { SimpleDateFormat("EEE", Locale(currentLang.code)) }
    val past7DaysStudy = remember(sessions, currentLang) {
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

    // Tasks
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks
    val taskCompletionRate = if (totalTasks > 0) ((completedTasks.toFloat() / totalTasks) * 100).toInt() else 0

    // Expenses
    val spentMonth = expenses.filter { it.dateTimestamp >= monthStart }.sumOf { it.amount }
    val catExpenses = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }

    // Habits
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayHabitsDone = habits.count { h -> logs.any { it.habitId == h.id && it.dateString == todayKey } }
    val habitRate = if (habits.isNotEmpty()) ((todayHabitsDone.toFloat() / habits.size) * 100).toInt() else 0
    val maxStreak = habits.maxOfOrNull { it.bestStreak } ?: 0
    val avgStreak = if (habits.isNotEmpty()) habits.map { it.currentStreak }.average().toInt() else 0

    Scaffold(
        modifier = modifier.testTag("statistics_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = strings.analyticsAndInsights,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // STUDY TAB
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                icon = "⏱️",
                                label = strings.hoursToday,
                                value = "${todayStudyMin / 60}h ${todayStudyMin % 60}m",
                                accentColor = IndigoLight,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = "📅",
                                label = strings.hoursThisWeek,
                                value = "${weekStudyMin / 60}h ${weekStudyMin % 60}m",
                                accentColor = CyanAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        StudyTrackCard {
                            Text(
                                text = strings.studyEvolution,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            SimpleBarChart(data = past7DaysStudy, barColor = IndigoLight)
                        }
                    }

                    item {
                        StudyTrackCard {
                            Text(
                                text = strings.subjectBreakdown,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            subjectCounts.forEach { (subject, mins) ->
                                val ratio = if (monthStudyMin > 0) (mins.toFloat() / monthStudyMin).coerceIn(0f, 1f) else 0f
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = subject, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "${mins / 60}h ${mins % 60}m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = IndigoPrimary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TASKS TAB
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                icon = "✅",
                                label = strings.completed,
                                value = "$completedTasks",
                                accentColor = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = "⏳",
                                label = strings.pending,
                                value = "$pendingTasks",
                                accentColor = AmberAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        StudyTrackCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.taskCompletionRateTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$completedTasks ${strings.ofPages} $totalTasks ${strings.tasksCheckedOffDesc}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                ProductivityGauge(score = taskCompletionRate, size = 90.dp)
                            }
                        }
                    }
                }
                2 -> {
                    // EXPENSES TAB
                    item {
                        StudyTrackCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = strings.monthlySpendingTitle, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", spentMonth)}",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = strings.budgetTargetTitle, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", profile.monthlyBudget)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        StudyTrackCard {
                            Text(
                                text = strings.spendingByCategoryTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            catExpenses.forEach { (cat, amount) ->
                                val ratio = if (spentMonth > 0) (amount / spentMonth).toFloat().coerceIn(0f, 1f) else 0f
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${cat.emoji} ${cat.label}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "$${String.format(Locale.US, "%.2f", amount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // HABITS TAB
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                icon = "🔥",
                                label = strings.avgStreakLabel,
                                value = "$avgStreak ${strings.days}",
                                accentColor = AmberAccent,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = "🏆",
                                label = strings.bestStreakLabel,
                                value = "$maxStreak ${strings.days}",
                                accentColor = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        StudyTrackCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.todayHabitCompletionTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$todayHabitsDone ${strings.ofPages} ${habits.size} ${strings.habitsCheckedOffDesc}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                ProductivityGauge(score = habitRate, size = 90.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
