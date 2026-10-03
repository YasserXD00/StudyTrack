package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Student",
    val avatarEmoji: String = "🎓",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val lastActiveDate: String = "",
    val dailyStudyGoalMinutes: Int = 120, // default 2 hours
    val monthlyBudget: Double = 300.0,
    val currencyCode: String = "USD",
    val isDarkMode: Boolean = false,
    val languageCode: String = "en"
) {
    val level: Int
        get() = (totalXp / 100) + 1

    val currentLevelXp: Int
        get() = totalXp % 100

    val nextLevelTargetXp: Int
        get() = 100
}

@Entity(tableName = "achievements")
data class AchievementItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val title: String,
    val description: String,
    val emoji: String,
    val xpReward: Int = 20,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
