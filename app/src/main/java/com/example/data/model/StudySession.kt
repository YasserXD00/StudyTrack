package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val durationMinutes: Int,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isPomodoro: Boolean = false
)
