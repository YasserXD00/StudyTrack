package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority(val label: String, val emoji: String) {
    URGENT("Urgent", "🔴"),
    IMPORTANT("Important", "🟠"),
    NORMAL("Normal", "🟢")
}

enum class TaskCategory(val label: String, val emoji: String) {
    STUDIES("Studies", "📚"),
    WORK("Work", "💼"),
    PERSONAL("Personal", "🏠"),
    GOALS("Goals", "🎯")
}

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long = System.currentTimeMillis(),
    val priority: TaskPriority = TaskPriority.NORMAL,
    val category: TaskCategory = TaskCategory.STUDIES,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
