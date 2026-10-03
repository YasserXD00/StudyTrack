package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.StudyTrackRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getAppStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProductivityMetrics(
    val score: Int = 82,
    val taskPart: Int = 24,    // max 30
    val studyPart: Int = 26,   // max 30
    val habitPart: Int = 16,   // max 20
    val goalPart: Int = 16     // max 20
)

data class PomodoroState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val selectedSubject: String = "Computer Science",
    val presetMinutes: Int = 25,
    val completedSessionsCount: Int = 0
)

private data class IntermediateActivityState(
    val tasks: List<TaskItem>,
    val sessions: List<StudySession>,
    val goals: List<GoalItem>
)

class StudyTrackViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = StudyTrackRepository(db)

    // Data streams
    val tasks: StateFlow<List<TaskItem>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions: StateFlow<List<StudySession>> = repository.studySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalItem>> = repository.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goalSteps: StateFlow<List<GoalStep>> = repository.goalSteps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitItem>> = repository.habits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogs: StateFlow<List<HabitLog>> = repository.habitLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseItem>> = repository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val books: StateFlow<List<BookItem>> = repository.books
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vocabulary: StateFlow<List<VocabularyItem>> = repository.vocabulary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<UserProfile> = repository.profile
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val achievements: StateFlow<List<AchievementItem>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Celebration message banner
    private val _celebrationMessage = MutableStateFlow<String?>(null)
    val celebrationMessage: StateFlow<String?> = _celebrationMessage.asStateFlow()

    // Language State (Supports English, Arabic, French, Russian, German, Chinese, Japanese)
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Pomodoro Timer State
    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = repository.profile.firstOrNull()
            if (existing == null) {
                repository.resetToZero("Student")
            } else {
                _currentLanguage.value = AppLanguage.fromCode(existing.languageCode)
            }
        }
        viewModelScope.launch {
            repository.profile.filterNotNull().collect { userProfile ->
                val lang = AppLanguage.fromCode(userProfile.languageCode)
                if (_currentLanguage.value != lang) {
                    _currentLanguage.value = lang
                }
            }
        }
    }

    fun showCelebration(msg: String) {
        _celebrationMessage.value = msg
        viewModelScope.launch {
            delay(3500)
            if (_celebrationMessage.value == msg) {
                _celebrationMessage.value = null
            }
        }
    }

    fun clearCelebration() {
        _celebrationMessage.value = null
    }

    // Combine streams into Productivity Score
    private val activityGroupFlow = combine(tasks, studySessions, goals) { t, s, g ->
        IntermediateActivityState(t, s, g)
    }

    val productivityMetrics: StateFlow<ProductivityMetrics> = combine(
        activityGroupFlow, habits, habitLogs, profile
    ) { activity, allHabits, logs, userProfile ->
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(Date())
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 1. Tasks part (30%)
        val taskPart = if (activity.tasks.isEmpty()) {
            0
        } else {
            val completed = activity.tasks.count { it.isCompleted }
            val ratio = completed.toFloat() / activity.tasks.size
            (ratio * 30).toInt().coerceIn(0, 30)
        }

        // 2. Study part (30%)
        val todayStudyMin = activity.sessions
            .filter { it.dateTimestamp >= todayStart }
            .sumOf { it.durationMinutes }
        val targetMin = userProfile.dailyStudyGoalMinutes.coerceAtLeast(30)
        val studyRatio = (todayStudyMin.toFloat() / targetMin).coerceIn(0f, 1f)
        val studyPart = (studyRatio * 30).toInt()

        // 3. Habits part (20%)
        val todayDoneHabits = logs.count { it.dateString == todayStr }
        val habitPart = if (allHabits.isEmpty()) {
            0
        } else {
            val habitRatio = (todayDoneHabits.toFloat() / allHabits.size).coerceIn(0f, 1f)
            (habitRatio * 20).toInt()
        }

        // 4. Goals part (20%)
        val goalPart = if (activity.goals.isEmpty()) {
            0
        } else {
            val avgProgress = activity.goals.map { it.progress }.average().toFloat() / 100f
            (avgProgress * 20).toInt().coerceIn(0, 20)
        }

        val totalScore = (taskPart + studyPart + habitPart + goalPart).coerceIn(0, 100)
        ProductivityMetrics(
            score = totalScore,
            taskPart = taskPart,
            studyPart = studyPart,
            habitPart = habitPart,
            goalPart = goalPart
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductivityMetrics())

    // Pomodoro Timer Controls
    fun setPomodoroPreset(minutes: Int) {
        stopTimer()
        _pomodoroState.update {
            it.copy(
                presetMinutes = minutes,
                totalSeconds = minutes * 60,
                remainingSeconds = minutes * 60,
                isBreak = false,
                isRunning = false
            )
        }
    }

    fun setPomodoroSubject(subject: String) {
        _pomodoroState.update { it.copy(selectedSubject = subject) }
    }

    fun startPomodoro() {
        if (_pomodoroState.value.isRunning) return
        _pomodoroState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_pomodoroState.value.remainingSeconds > 0 && _pomodoroState.value.isRunning) {
                delay(1000)
                _pomodoroState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            if (_pomodoroState.value.remainingSeconds <= 0) {
                handlePomodoroComplete()
            }
        }
    }

    fun pausePomodoro() {
        timerJob?.cancel()
        _pomodoroState.update { it.copy(isRunning = false) }
    }

    fun resumePomodoro() {
        startPomodoro()
    }

    fun resetPomodoro() {
        stopTimer()
        val total = _pomodoroState.value.presetMinutes * 60
        _pomodoroState.update {
            it.copy(
                isRunning = false,
                isBreak = false,
                totalSeconds = total,
                remainingSeconds = total
            )
        }
    }

    fun skipPomodoro() {
        stopTimer()
        if (!_pomodoroState.value.isBreak) {
            val breakSec = if (_pomodoroState.value.presetMinutes >= 50) 10 * 60 else 5 * 60
            _pomodoroState.update {
                it.copy(
                    isBreak = true,
                    isRunning = false,
                    totalSeconds = breakSec,
                    remainingSeconds = breakSec
                )
            }
        } else {
            val focusSec = _pomodoroState.value.presetMinutes * 60
            _pomodoroState.update {
                it.copy(
                    isBreak = false,
                    isRunning = false,
                    totalSeconds = focusSec,
                    remainingSeconds = focusSec
                )
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun handlePomodoroComplete() {
        stopTimer()
        val wasBreak = _pomodoroState.value.isBreak
        if (!wasBreak) {
            val duration = _pomodoroState.value.presetMinutes
            val subject = _pomodoroState.value.selectedSubject
            viewModelScope.launch {
                repository.insertStudySession(
                    StudySession(
                        subject = subject,
                        durationMinutes = duration,
                        dateTimestamp = System.currentTimeMillis(),
                        notes = "Completed Pomodoro Focus session",
                        isPomodoro = true
                    )
                )
                showCelebration("🎉 Focus session completed! +20 XP")
            }
            val breakSec = if (duration >= 50) 10 * 60 else 5 * 60
            _pomodoroState.update {
                it.copy(
                    isBreak = true,
                    isRunning = false,
                    totalSeconds = breakSec,
                    remainingSeconds = breakSec,
                    completedSessionsCount = it.completedSessionsCount + 1
                )
            }
        } else {
            showCelebration("☕ Break finished! Ready for next focus session?")
            val focusSec = _pomodoroState.value.presetMinutes * 60
            _pomodoroState.update {
                it.copy(
                    isBreak = false,
                    isRunning = false,
                    totalSeconds = focusSec,
                    remainingSeconds = focusSec
                )
            }
        }
    }

    // Task Actions
    fun addTask(title: String, description: String, dueDate: Long, priority: TaskPriority, category: TaskCategory) {
        viewModelScope.launch {
            repository.insertTask(
                TaskItem(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    category = category
                )
            )
            val strings = getAppStrings(_currentLanguage.value)
            showCelebration(strings.taskAddedCelebration)
        }
    }

    fun updateTask(task: TaskItem) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            val isNowCompleted = repository.toggleTaskCompleted(task)
            if (isNowCompleted) {
                val strings = getAppStrings(_currentLanguage.value)
                showCelebration(strings.taskCompletedXp)
            }
        }
    }

    // Study Actions
    fun logStudySession(subject: String, durationMinutes: Int, notes: String) {
        viewModelScope.launch {
            repository.insertStudySession(
                StudySession(
                    subject = subject,
                    durationMinutes = durationMinutes,
                    dateTimestamp = System.currentTimeMillis(),
                    notes = notes,
                    isPomodoro = false
                )
            )
            showCelebration("📚 Study session recorded! +20 XP")
        }
    }

    fun deleteStudySession(session: StudySession) {
        viewModelScope.launch {
            repository.deleteStudySession(session)
        }
    }

    // Goal Actions
    fun addGoal(title: String, description: String, category: String, deadline: Long, steps: List<String>) {
        viewModelScope.launch {
            val goalId = repository.insertGoal(
                GoalItem(
                    title = title,
                    description = description,
                    category = category,
                    deadline = deadline,
                    progress = 0
                )
            )
            steps.forEachIndexed { index, stepTitle ->
                if (stepTitle.isNotBlank()) {
                    repository.insertGoalStep(
                        GoalStep(
                            goalId = goalId,
                            title = stepTitle,
                            orderIndex = index,
                            isCompleted = false
                        )
                    )
                }
            }
            showCelebration("🎯 Goal created!")
        }
    }

    fun deleteGoal(goal: GoalItem) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addGoalStep(goalId: Long, title: String) {
        viewModelScope.launch {
            val existing = goalSteps.value.filter { it.goalId == goalId }
            repository.insertGoalStep(
                GoalStep(
                    goalId = goalId,
                    title = title,
                    orderIndex = existing.size,
                    isCompleted = false
                )
            )
        }
    }

    fun toggleGoalStep(step: GoalStep) {
        viewModelScope.launch {
            repository.toggleGoalStep(step)
            if (!step.isCompleted) {
                showCelebration("⭐ Step checked off! +15 XP")
            }
        }
    }

    fun deleteGoalStep(step: GoalStep) {
        viewModelScope.launch {
            repository.deleteGoalStep(step)
        }
    }

    // Habit Actions
    fun addHabit(title: String, emoji: String, category: String) {
        viewModelScope.launch {
            repository.insertHabit(
                HabitItem(
                    title = title,
                    emoji = if (emoji.isNotBlank()) emoji else "🌱",
                    category = category
                )
            )
            showCelebration("🌱 Habit added!")
        }
    }

    fun deleteHabit(habit: HabitItem) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun toggleHabitToday(habitId: Long) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(Date())
        viewModelScope.launch {
            repository.toggleHabitForDate(habitId, todayStr)
            showCelebration("✨ Habit updated! +5 XP")
        }
    }

    // Expense Actions
    fun addExpense(title: String, amount: Double, category: ExpenseCategory, notes: String) {
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseItem(
                    title = title,
                    amount = amount,
                    category = category,
                    dateTimestamp = System.currentTimeMillis(),
                    notes = notes
                )
            )
            showCelebration("💰 Expense recorded!")
        }
    }

    fun deleteExpense(expense: ExpenseItem) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Book Actions
    fun addBook(title: String, author: String, totalPages: Int, pagesRead: Int, rating: Int, status: BookStatus) {
        viewModelScope.launch {
            repository.insertBook(
                BookItem(
                    title = title,
                    author = author,
                    totalPages = totalPages,
                    pagesRead = pagesRead,
                    rating = rating,
                    startDate = System.currentTimeMillis(),
                    status = status
                )
            )
            showCelebration("📖 Book added!")
        }
    }

    fun updateBookPages(book: BookItem, newPages: Int) {
        viewModelScope.launch {
            repository.updateBookPages(book, newPages)
            showCelebration("📚 Reading progress saved! +15 XP")
        }
    }

    fun deleteBook(book: BookItem) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    // English Vocabulary Actions
    fun addVocabulary(word: String, meaning: String, example: String, skill: EnglishSkill) {
        viewModelScope.launch {
            repository.insertVocabulary(
                VocabularyItem(
                    word = word,
                    meaning = meaning,
                    example = example,
                    skill = skill
                )
            )
            showCelebration("🇬🇧 New word added! +10 XP")
        }
    }

    fun toggleVocabularyMastered(item: VocabularyItem) {
        viewModelScope.launch {
            repository.toggleVocabularyMastered(item)
            if (!item.isMastered) {
                showCelebration("🎉 Word mastered! +10 XP")
            }
        }
    }

    fun deleteVocabulary(item: VocabularyItem) {
        viewModelScope.launch {
            repository.deleteVocabulary(item)
        }
    }

    // Profile Settings
    fun updateProfile(name: String, studyGoalMinutes: Int, monthlyBudget: Double, currencyCode: String, isDarkMode: Boolean) {
        viewModelScope.launch {
            val current = profile.value
            repository.updateProfile(
                current.copy(
                    name = name,
                    dailyStudyGoalMinutes = studyGoalMinutes,
                    monthlyBudget = monthlyBudget,
                    currencyCode = currencyCode,
                    isDarkMode = isDarkMode
                )
            )
            showCelebration("⚙️ Settings updated!")
        }
    }

    fun recordPomodoroCompletion(durationMinutes: Int, subject: String) {
        viewModelScope.launch {
            repository.insertStudySession(
                StudySession(
                    subject = subject,
                    durationMinutes = durationMinutes,
                    dateTimestamp = System.currentTimeMillis(),
                    notes = "Completed Pomodoro Focus session",
                    isPomodoro = true
                )
            )
            showCelebration("🎉 Focus session completed! +20 XP")
        }
    }

    fun resetData() {
        viewModelScope.launch {
            val currentLang = _currentLanguage.value.code
            val currentName = repository.profile.firstOrNull()?.name?.ifBlank { "Student" } ?: "Student"
            repository.resetToZero(currentName)
            val p = repository.profile.firstOrNull() ?: UserProfile()
            repository.updateProfile(p.copy(languageCode = currentLang))
            showCelebration("✨ Reset everything to 0!")
        }
    }

    fun resetEverythingToZero(username: String = "Student") {
        viewModelScope.launch {
            val currentLang = _currentLanguage.value.code
            repository.resetToZero(username)
            val p = repository.profile.firstOrNull() ?: UserProfile()
            repository.updateProfile(p.copy(languageCode = currentLang))
            showCelebration("✨ Reset everything to 0 for $username!")
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        viewModelScope.launch {
            val current = repository.profile.firstOrNull() ?: UserProfile()
            repository.updateProfile(current.copy(languageCode = language.code))
            val msg = when (language) {
                AppLanguage.ARABIC -> "🇸🇦 تم تغيير اللغة إلى العربية"
                AppLanguage.FRENCH -> "🇫🇷 Langue changée en Français"
                AppLanguage.RUSSIAN -> "🇷🇺 Язык изменен на Русский"
                AppLanguage.GERMAN -> "🇩🇪 Sprache auf Deutsch geändert"
                AppLanguage.CHINESE -> "🇨🇳 语言已切换为中文"
                AppLanguage.JAPANESE -> "🇯🇵 言語を日本語に変更しました"
                AppLanguage.SPANISH -> "🇪🇸 Idioma cambiado a Español"
                AppLanguage.ENGLISH -> "🇺🇸 Language switched to English"
            }
            showCelebration(msg)
        }
    }
}

// Backward compatibility alias
typealias EduPulseViewModel = StudyTrackViewModel
