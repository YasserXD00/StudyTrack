package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookStatus
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StudyTrackCard
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.StudyTrackViewModel
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: StudyTrackViewModel,
    onNavigateToAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val profile by viewModel.profile.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()
    val books by viewModel.books.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val logs by viewModel.habitLogs.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val totalStudyMinutes = sessions.sumOf { it.durationMinutes }
    val totalStudyHours = totalStudyMinutes / 60
    val completedBooksCount = books.count { it.status == BookStatus.FINISHED }
    val completedGoalsCount = goals.count { it.progress == 100 }
    val completedTasksCount = tasks.count { it.isCompleted }

    Scaffold(
        modifier = modifier.testTag("profile_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                StudyTrackCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = profile.avatarEmoji, fontSize = 32.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "🏆 ${strings.level} ${profile.level} • ${profile.totalXp} ${strings.xp}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { profile.currentLevelXp / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AmberAccent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Lifetime Stats Section
            item {
                SectionHeader(title = strings.lifetimeAchievements)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = "⏱️",
                        label = strings.studyTime,
                        value = "${totalStudyHours}h ${totalStudyMinutes % 60}m",
                        accentColor = IndigoLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "🔥",
                        label = strings.currentStreak,
                        value = "${profile.streakDays} ${strings.days}",
                        accentColor = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = "📖",
                        label = strings.booksRead,
                        value = "$completedBooksCount",
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "🎯",
                        label = strings.goalsFinished,
                        value = "$completedGoalsCount",
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Access to Achievements
            item {
                StudyTrackCard(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAchievements() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🏆", fontSize = 28.sp)
                            Column {
                                Text(
                                    text = strings.badgesAndTrophies,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "View rank progression and trophy collection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Settings & Preferences
            item {
                SectionHeader(title = strings.appSettings)
                StudyTrackCard {
                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = IndigoLight)
                            Column {
                                Text(
                                    text = strings.darkMode,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (profile.isDarkMode) "Enabled" else "Light Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = profile.isDarkMode,
                            onCheckedChange = { isDark ->
                                viewModel.updateProfile(
                                    name = profile.name,
                                    studyGoalMinutes = profile.dailyStudyGoalMinutes,
                                    monthlyBudget = profile.monthlyBudget,
                                    currencyCode = profile.currencyCode,
                                    isDarkMode = isDark
                                )
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Daily Study Target
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = CyanAccent)
                            Column {
                                Text(
                                    text = strings.dailyStudyGoal,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${profile.dailyStudyGoalMinutes} ${strings.minutes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = { showEditDialog = true }) {
                            Text(strings.edit)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Monthly Budget Target
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = EmeraldSuccess)
                            Column {
                                Text(
                                    text = strings.monthlyBudget,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val currency = com.example.data.model.Currency.fromCode(profile.currencyCode)
                                Text(
                                    text = "${currency.symbol}${String.format(Locale.US, "%.2f", profile.monthlyBudget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = { showEditDialog = true }) {
                            Text(strings.edit)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Language Selection Setting
                    val currentLang = LocalAppLanguage.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = IndigoPrimary)
                            Column {
                                Text(
                                    text = "${strings.language} / اللغة",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentLang.flag} ${currentLang.nativeName} (${currentLang.displayName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = { showLanguageDialog = true }) {
                            Text(strings.changeLanguage)
                        }
                    }
                }
            }

            // Data Management
            item {
                SectionHeader(title = strings.dataAndBackup)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportSummary)
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.resetDemo)
                    }
                }
            }

            // Slogan & Branding Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "StudyTrack",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Track. Learn. Grow. 🚀",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v1.0 • Modern Student Productivity Dashboard",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var nameInput by remember { mutableStateOf(profile.name) }
        var studyGoalInput by remember { mutableStateOf(profile.dailyStudyGoalMinutes.toString()) }
        var budgetInput by remember { mutableStateOf(profile.monthlyBudget.toString()) }
        var currencyInput by remember { mutableStateOf(profile.currencyCode) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(strings.editProfileTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(strings.yourNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = studyGoalInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) studyGoalInput = it },
                        label = { Text(strings.dailyStudyTargetMinutes) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Monthly Budget (${currencyInput})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Currency", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.data.model.Currency.values().forEach { curr ->
                            FilterChip(
                                selected = currencyInput == curr.code,
                                onClick = { currencyInput = curr.code },
                                label = { Text("${curr.symbol} ${curr.code}") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val studyGoal = studyGoalInput.toIntOrNull() ?: 120
                        val budget = budgetInput.toDoubleOrNull() ?: 350.0
                        viewModel.updateProfile(
                            name = nameInput.ifBlank { "Student" },
                            studyGoalMinutes = studyGoal,
                            monthlyBudget = budget,
                            currencyCode = currencyInput,
                            isDarkMode = profile.isDarkMode
                        )
                        showEditDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showExportDialog) {
        val summaryText = """
            🚀 ${strings.appName} ${strings.studentSummaryTitle}
            ${strings.yourNameLabel}: ${profile.name}
            ${strings.level}: ${profile.level} (${strings.xp}: ${profile.totalXp})
            ${strings.currentStreak}: ${profile.streakDays} ${strings.days}
            ${strings.studyTime}: ${totalStudyHours}h ${totalStudyMinutes % 60}m
            ${strings.completed}: $completedTasksCount
            ${strings.booksRead}: $completedBooksCount
            ${strings.goalsFinished}: $completedGoalsCount
            Generated by ${strings.appName} 🚀
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(strings.studentSummaryTitle) },
            text = {
                Text(text = summaryText, style = MaterialTheme.typography.bodySmall)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, summaryText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "${strings.appName} Progress")
                        context.startActivity(shareIntent)
                        showExportDialog = false
                    }
                ) {
                    Text(strings.shareProgressAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings.resetDemo) },
            text = {
                Text(strings.resetDemoConfirmDesc)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.resetDemo)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showLanguageDialog) {
        val currentLang = LocalAppLanguage.current
        LanguageSelectorDialog(
            currentLanguage = currentLang,
            onLanguageSelected = { lang ->
                viewModel.setLanguage(lang)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}
