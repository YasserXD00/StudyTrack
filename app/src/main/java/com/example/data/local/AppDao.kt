package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItem): Long

    @Update
    suspend fun update(task: TaskItem)

    @Delete
    suspend fun delete(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_sessions ORDER BY dateTimestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySession): Long

    @Delete
    suspend fun delete(session: StudySession)

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalItem>>

    @Query("SELECT * FROM goal_steps ORDER BY orderIndex ASC")
    fun getAllSteps(): Flow<List<GoalStep>>

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY orderIndex ASC")
    fun getStepsForGoal(goalId: Long): Flow<List<GoalStep>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalItem): Long

    @Update
    suspend fun updateGoal(goal: GoalItem)

    @Delete
    suspend fun deleteGoal(goal: GoalItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: GoalStep): Long

    @Update
    suspend fun updateStep(step: GoalStep)

    @Delete
    suspend fun deleteStep(step: GoalStep)

    @Query("DELETE FROM goal_steps")
    suspend fun deleteAllSteps()

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<HabitItem>>

    @Query("SELECT * FROM habit_logs")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitItem): Long

    @Update
    suspend fun updateHabit(habit: HabitItem)

    @Delete
    suspend fun deleteHabit(habit: HabitItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteLog(habitId: Long, dateString: String)

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateTimestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseItem): Long

    @Delete
    suspend fun delete(expense: ExpenseItem)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY status ASC, id DESC")
    fun getAllBooks(): Flow<List<BookItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookItem): Long

    @Update
    suspend fun update(book: BookItem)

    @Delete
    suspend fun delete(book: BookItem)

    @Query("DELETE FROM books")
    suspend fun deleteAll()
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY dateAdded DESC")
    fun getAllVocabulary(): Flow<List<VocabularyItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VocabularyItem): Long

    @Update
    suspend fun update(item: VocabularyItem)

    @Delete
    suspend fun delete(item: VocabularyItem)

    @Query("DELETE FROM vocabulary")
    suspend fun deleteAll()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun getAllAchievements(): Flow<List<AchievementItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AchievementItem>)

    @Update
    suspend fun update(item: AchievementItem)

    @Query("UPDATE achievements SET isUnlocked = 0, unlockedAt = NULL")
    suspend fun resetAllToLocked()

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}
