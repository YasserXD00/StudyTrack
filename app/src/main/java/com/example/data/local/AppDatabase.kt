package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskItem::class,
        StudySession::class,
        GoalItem::class,
        GoalStep::class,
        HabitItem::class,
        HabitLog::class,
        ExpenseItem::class,
        BookItem::class,
        VocabularyItem::class,
        UserProfile::class,
        AchievementItem::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun studyDao(): StudyDao
    abstract fun goalDao(): GoalDao
    abstract fun habitDao(): HabitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun bookDao(): BookDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun profileDao(): ProfileDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studytrack_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            resetToZero(database, "Student")
        }

        suspend fun resetToZero(database: AppDatabase, username: String = "Student") {
            // 1. Clear all existing records to zero
            database.taskDao().deleteAll()
            database.studyDao().deleteAll()
            database.goalDao().deleteAllSteps()
            database.goalDao().deleteAllGoals()
            database.habitDao().deleteAllLogs()
            database.habitDao().deleteAllHabits()
            database.expenseDao().deleteAll()
            database.bookDao().deleteAll()
            database.vocabularyDao().deleteAll()
            database.achievementDao().deleteAll()

            // 2. Insert clean User Profile with 0 XP and 0 streak
            database.profileDao().insertOrUpdate(
                UserProfile(
                    id = 1,
                    name = username,
                    avatarEmoji = "🎓",
                    totalXp = 0,
                    streakDays = 0,
                    lastActiveDate = "",
                    dailyStudyGoalMinutes = 120,
                    monthlyBudget = 350.0,
                    isDarkMode = false,
                    languageCode = "en"
                )
            )

            // 3. Insert baseline achievements - all locked (0 unlocked)
            val achievements = listOf(
                AchievementItem(key = "first_step", title = "First Step", description = "Complete your first task", emoji = "🏅", xpReward = 10, isUnlocked = false),
                AchievementItem(key = "streak_7", title = "7-Day Streak", description = "Maintain a 7-day streak", emoji = "🔥", xpReward = 50, isUnlocked = false),
                AchievementItem(key = "bookworm", title = "Bookworm", description = "Finish 5 books", emoji = "📚", xpReward = 50, isUnlocked = false),
                AchievementItem(key = "goal_crusher", title = "Goal Crusher", description = "Complete 10 goals", emoji = "🎯", xpReward = 100, isUnlocked = false),
                AchievementItem(key = "focus_master", title = "Focus Master", description = "Complete 20 focus sessions", emoji = "⏱️", xpReward = 50, isUnlocked = false),
                AchievementItem(key = "budget_master", title = "Budget Master", description = "Track 5 expenses", emoji = "💰", xpReward = 20, isUnlocked = false),
                AchievementItem(key = "word_collector", title = "Word Collector", description = "Learn 10 English words", emoji = "🇬🇧", xpReward = 30, isUnlocked = false),
                AchievementItem(key = "habit_starter", title = "Habit Starter", description = "Check off 5 habits", emoji = "🌱", xpReward = 20, isUnlocked = false)
            )
            database.achievementDao().insertAll(achievements)
        }
    }
}
