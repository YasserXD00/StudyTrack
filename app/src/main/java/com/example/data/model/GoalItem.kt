package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Studies",
    val deadline: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "goal_steps",
    foreignKeys = [
        ForeignKey(
            entity = GoalItem::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class GoalStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)
