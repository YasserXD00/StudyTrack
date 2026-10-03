package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class StudyTrackRepository(private val db: AppDatabase) {

    // Flows
    val tasks: Flow<List<TaskItem>> = db.taskDao().getAllTasks()
    val studySessions: Flow<List<StudySession>> = db.studyDao().getAllSessions()
    val goals: Flow<List<GoalItem>> = db.goalDao().getAllGoals()
    val goalSteps: Flow<List<GoalStep>> = db.goalDao().getAllSteps()
    val habits: Flow<List<HabitItem>> = db.habitDao().getAllHabits()
    val habitLogs: Flow<List<HabitLog>> = db.habitDao().getAllLogs()
    val expenses: Flow<List<ExpenseItem>> = db.expenseDao().getAllExpenses()
    val books: Flow<List<BookItem>> = db.bookDao().getAllBooks()
    val vocabulary: Flow<List<VocabularyItem>> = db.vocabularyDao().getAllVocabulary()
    val profile: Flow<UserProfile?> = db.profileDao().getProfile()
    val achievements: Flow<List<AchievementItem>> = db.achievementDao().getAllAchievements()

    // Tasks
    suspend fun insertTask(task: TaskItem): Long = db.taskDao().insert(task)
    suspend fun updateTask(task: TaskItem) = db.taskDao().update(task)
    suspend fun deleteTask(task: TaskItem) = db.taskDao().delete(task)
    suspend fun deleteTaskById(id: Long) = db.taskDao().deleteById(id)

    suspend fun toggleTaskCompleted(task: TaskItem): Boolean {
        val newStatus = !task.isCompleted
        db.taskDao().update(task.copy(isCompleted = newStatus))
        if (newStatus) {
            addXp(10)
            checkAchievements()
        }
        return newStatus
    }

    // Study
    suspend fun insertStudySession(session: StudySession): Long {
        val id = db.studyDao().insert(session)
        addXp(20)
        checkAchievements()
        return id
    }
    suspend fun deleteStudySession(session: StudySession) = db.studyDao().delete(session)

    // Goals
    suspend fun insertGoal(goal: GoalItem): Long = db.goalDao().insertGoal(goal)
    suspend fun updateGoal(goal: GoalItem) = db.goalDao().updateGoal(goal)
    suspend fun deleteGoal(goal: GoalItem) = db.goalDao().deleteGoal(goal)

    suspend fun insertGoalStep(step: GoalStep): Long {
        val id = db.goalDao().insertStep(step)
        recalculateGoalProgress(step.goalId)
        return id
    }

    suspend fun toggleGoalStep(step: GoalStep) {
        val updated = step.copy(isCompleted = !step.isCompleted)
        db.goalDao().updateStep(updated)
        recalculateGoalProgress(step.goalId)
        if (updated.isCompleted) {
            addXp(15)
        }
    }

    suspend fun deleteGoalStep(step: GoalStep) {
        db.goalDao().deleteStep(step)
        recalculateGoalProgress(step.goalId)
    }

    private suspend fun recalculateGoalProgress(goalId: Long) {
        val steps = db.goalDao().getStepsForGoal(goalId).firstOrNull() ?: emptyList()
        if (steps.isNotEmpty()) {
            val completed = steps.count { it.isCompleted }
            val progressPercent = ((completed.toFloat() / steps.size) * 100).toInt()
            val allGoals = db.goalDao().getAllGoals().firstOrNull() ?: emptyList()
            val goal = allGoals.firstOrNull { it.id == goalId }
            if (goal != null) {
                db.goalDao().updateGoal(goal.copy(progress = progressPercent))
                if (progressPercent == 100 && goal.progress < 100) {
                    addXp(50)
                    checkAchievements()
                }
            }
        }
    }

    // Habits
    suspend fun insertHabit(habit: HabitItem): Long = db.habitDao().insertHabit(habit)
    suspend fun updateHabit(habit: HabitItem) = db.habitDao().updateHabit(habit)
    suspend fun deleteHabit(habit: HabitItem) = db.habitDao().deleteHabit(habit)

    suspend fun toggleHabitForDate(habitId: Long, dateString: String) {
        val allLogs = db.habitDao().getAllLogs().firstOrNull() ?: emptyList()
        val existing = allLogs.firstOrNull { it.habitId == habitId && it.dateString == dateString }
        if (existing != null) {
            db.habitDao().deleteLog(habitId, dateString)
        } else {
            db.habitDao().insertLog(HabitLog(habitId = habitId, dateString = dateString, isCompleted = true))
            addXp(5)
            checkAchievements()
        }
    }

    // Expenses
    suspend fun insertExpense(expense: ExpenseItem): Long {
        val id = db.expenseDao().insert(expense)
        checkAchievements()
        return id
    }
    suspend fun deleteExpense(expense: ExpenseItem) = db.expenseDao().delete(expense)

    // Books
    suspend fun insertBook(book: BookItem): Long = db.bookDao().insert(book)
    suspend fun updateBook(book: BookItem) {
        db.bookDao().update(book)
        if (book.status == BookStatus.FINISHED) {
            checkAchievements()
        }
    }
    suspend fun deleteBook(book: BookItem) = db.bookDao().delete(book)

    suspend fun updateBookPages(book: BookItem, newPages: Int) {
        val clampedPages = newPages.coerceIn(0, book.totalPages)
        val newStatus = if (clampedPages >= book.totalPages && book.totalPages > 0) {
            BookStatus.FINISHED
        } else if (clampedPages > 0) {
            BookStatus.READING
        } else {
            book.status
        }
        val finishTime = if (newStatus == BookStatus.FINISHED && book.finishDate == null) System.currentTimeMillis() else book.finishDate
        db.bookDao().update(book.copy(pagesRead = clampedPages, status = newStatus, finishDate = finishTime))
        addXp(15)
        checkAchievements()
    }

    // English Vocabulary
    suspend fun insertVocabulary(item: VocabularyItem): Long {
        val id = db.vocabularyDao().insert(item)
        addXp(10)
        checkAchievements()
        return id
    }
    suspend fun updateVocabulary(item: VocabularyItem) = db.vocabularyDao().update(item)
    suspend fun deleteVocabulary(item: VocabularyItem) = db.vocabularyDao().delete(item)
    suspend fun toggleVocabularyMastered(item: VocabularyItem) {
        val newStatus = !item.isMastered
        db.vocabularyDao().update(item.copy(isMastered = newStatus))
        if (newStatus) {
            addXp(10)
        }
    }

    // Profile & XP
    suspend fun updateProfile(profile: UserProfile) = db.profileDao().insertOrUpdate(profile)

    suspend fun addXp(amount: Int) {
        val currentProfile = db.profileDao().getProfile().firstOrNull() ?: UserProfile()
        val newXp = currentProfile.totalXp + amount
        db.profileDao().insertOrUpdate(currentProfile.copy(totalXp = newXp))
    }

    // Achievements check
    suspend fun checkAchievements() {
        val currentAchievements = db.achievementDao().getAllAchievements().firstOrNull() ?: emptyList()
        val allTasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
        val allBooks = db.bookDao().getAllBooks().firstOrNull() ?: emptyList()
        val allGoals = db.goalDao().getAllGoals().firstOrNull() ?: emptyList()
        val allSessions = db.studyDao().getAllSessions().firstOrNull() ?: emptyList()
        val allExpenses = db.expenseDao().getAllExpenses().firstOrNull() ?: emptyList()
        val allVocab = db.vocabularyDao().getAllVocabulary().firstOrNull() ?: emptyList()
        val allLogs = db.habitDao().getAllLogs().firstOrNull() ?: emptyList()

        for (ach in currentAchievements) {
            if (ach.isUnlocked) continue
            val shouldUnlock = when (ach.key) {
                "first_step" -> allTasks.any { it.isCompleted }
                "streak_7" -> allLogs.size >= 7
                "bookworm" -> allBooks.count { it.status == BookStatus.FINISHED } >= 5
                "goal_crusher" -> allGoals.count { it.progress == 100 } >= 10
                "focus_master" -> allSessions.count { it.isPomodoro } >= 20
                "budget_master" -> allExpenses.size >= 5
                "word_collector" -> allVocab.size >= 10
                "habit_starter" -> allLogs.size >= 5
                else -> false
            }
            if (shouldUnlock) {
                db.achievementDao().update(ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
                addXp(ach.xpReward)
            }
        }
    }

    suspend fun resetToZero(username: String = "Student") {
        AppDatabase.resetToZero(db, username)
    }

    suspend fun resetToDefaultDemo() {
        AppDatabase.populateInitialData(db)
    }
}

// Backward compatibility alias
typealias EduPulseRepository = StudyTrackRepository
