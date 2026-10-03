package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.task.TaskFormDialog
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.StudyTrackTheme
import com.example.ui.viewmodel.StudyTrackViewModel

enum class EduScreen(val label: String, val icon: ImageVector, val emoji: String) {
    DASHBOARD("Home", Icons.Default.Dashboard, "🏠"),
    TASKS("Tasks", Icons.Default.Checklist, "📝"),
    STUDY("Study", Icons.Default.MenuBook, "📚"),
    GOALS("Goals", Icons.Default.TrackChanges, "🎯"),
    HABITS("Habits", Icons.Default.Eco, "🌱"),
    STATISTICS("Stats", Icons.Default.BarChart, "📊"),
    PROFILE("Profile", Icons.Default.Person, "👤"),

    // Sub-screens
    POMODORO("Focus", Icons.Default.Timer, "⏱️"),
    EXPENSES("Expenses", Icons.Default.AttachMoney, "💰"),
    BOOKS("Books", Icons.Default.Bookmark, "📖"),
    ENGLISH("English", Icons.Default.Language, "🇬🇧"),
    ACHIEVEMENTS("Trophies", Icons.Default.EmojiEvents, "🏆")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StudyTrackViewModel = viewModel()
            val profile by viewModel.profile.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val appStrings = remember(currentLanguage) { getAppStrings(currentLanguage) }
            val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(currentLanguage) {
                val locale = java.util.Locale(currentLanguage.code)
                java.util.Locale.setDefault(locale)
                val resources = context.resources
                val configuration = resources.configuration
                configuration.setLocale(locale)
                configuration.setLayoutDirection(locale)
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalAppLanguage provides currentLanguage,
                LocalAppStrings provides appStrings
            ) {
                StudyTrackTheme(darkTheme = profile.isDarkMode) {
                    StudyTrackApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduPulseApp(viewModel: StudyTrackViewModel) = StudyTrackApp(viewModel = viewModel)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTrackApp(viewModel: StudyTrackViewModel) {
    var currentScreen by remember { mutableStateOf(EduScreen.DASHBOARD) }

    // Quick Dialogs state from Dashboard
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showLogStudyDialog by remember { mutableStateOf(false) }

    val celebrationMessage by viewModel.celebrationMessage.collectAsState()

    // Handle back button for sub-screens
    if (currentScreen != EduScreen.DASHBOARD) {
        BackHandler {
            currentScreen = EduScreen.DASHBOARD
        }
    }

    val primaryNavScreens = listOf(
        EduScreen.DASHBOARD,
        EduScreen.TASKS,
        EduScreen.STUDY,
        EduScreen.GOALS,
        EduScreen.HABITS,
        EduScreen.STATISTICS,
        EduScreen.PROFILE
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            if (currentScreen in primaryNavScreens) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val strings = LocalAppStrings.current
                    primaryNavScreens.forEach { screen ->
                        val selected = currentScreen == screen
                        val labelText = when (screen) {
                            EduScreen.DASHBOARD -> strings.navHome
                            EduScreen.TASKS -> strings.navTasks
                            EduScreen.STUDY -> strings.navStudy
                            EduScreen.GOALS -> strings.navGoals
                            EduScreen.HABITS -> strings.navHabits
                            EduScreen.STATISTICS -> strings.navStats
                            EduScreen.PROFILE -> strings.navProfile
                            EduScreen.POMODORO -> strings.navFocus
                            EduScreen.EXPENSES -> strings.navExpenses
                            EduScreen.BOOKS -> strings.navBooks
                            EduScreen.ENGLISH -> strings.navEnglish
                            EduScreen.ACHIEVEMENTS -> strings.navTrophies
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Text(
                                    text = screen.emoji,
                                    fontSize = if (selected) 20.sp else 16.sp
                                )
                            },
                            label = {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            alwaysShowLabel = false,
                            modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen switching with smooth crossfade
            AnimatedContent(
                targetState = currentScreen,
                label = "screenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    EduScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTasks = { currentScreen = EduScreen.TASKS },
                        onNavigateToStudy = { currentScreen = EduScreen.STUDY },
                        onNavigateToPomodoro = { currentScreen = EduScreen.POMODORO },
                        onNavigateToGoals = { currentScreen = EduScreen.GOALS },
                        onNavigateToHabits = { currentScreen = EduScreen.HABITS },
                        onNavigateToExpenses = { currentScreen = EduScreen.EXPENSES },
                        onNavigateToBooks = { currentScreen = EduScreen.BOOKS },
                        onNavigateToEnglish = { currentScreen = EduScreen.ENGLISH },
                        onNavigateToAchievements = { currentScreen = EduScreen.ACHIEVEMENTS },
                        onOpenAddTaskDialog = { showAddTaskDialog = true },
                        onOpenAddExpenseDialog = { showAddExpenseDialog = true },
                        onOpenLogStudyDialog = { showLogStudyDialog = true }
                    )
                    EduScreen.TASKS -> TasksScreen(viewModel = viewModel)
                    EduScreen.STUDY -> StudyScreen(
                        viewModel = viewModel,
                        onNavigateToPomodoro = { currentScreen = EduScreen.POMODORO }
                    )
                    EduScreen.GOALS -> GoalsScreen(viewModel = viewModel)
                    EduScreen.HABITS -> HabitsScreen(viewModel = viewModel)
                    EduScreen.STATISTICS -> StatisticsScreen(viewModel = viewModel)
                    EduScreen.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToAchievements = { currentScreen = EduScreen.ACHIEVEMENTS }
                    )
                    EduScreen.POMODORO -> PomodoroScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = EduScreen.DASHBOARD }
                    )
                    EduScreen.EXPENSES -> ExpensesScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = EduScreen.DASHBOARD }
                    )
                    EduScreen.BOOKS -> BooksScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = EduScreen.DASHBOARD }
                    )
                    EduScreen.ENGLISH -> EnglishScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = EduScreen.DASHBOARD }
                    )
                    EduScreen.ACHIEVEMENTS -> AchievementsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = EduScreen.DASHBOARD }
                    )
                }
            }

            // Floating Celebration Toast / Banner
            AnimatedVisibility(
                visible = celebrationMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                celebrationMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("celebration_banner"),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            IconButton(
                                onClick = { viewModel.clearCelebration() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Dialogs
    if (showAddTaskDialog) {
        TaskFormDialog(
            task = null,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, dueDate, priority, category ->
                viewModel.addTask(title, desc, dueDate, priority, category)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        val currentProfile by viewModel.profile.collectAsState()
        val currency = com.example.data.model.Currency.fromCode(currentProfile.currencyCode)
        AddExpenseDialog(
            curSym = currency.symbol,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, amount, category, notes ->
                viewModel.addExpense(title, amount, category, notes)
                showAddExpenseDialog = false
            }
        )
    }

    if (showLogStudyDialog) {
        LogStudySessionDialog(
            onDismiss = { showLogStudyDialog = false },
            onConfirm = { subject, duration, notes ->
                viewModel.logStudySession(subject, duration, notes)
                showLogStudyDialog = false
            }
        )
    }
}
