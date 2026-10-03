package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExpenseCategory(val label: String, val emoji: String) {
    FOOD("Food", "🍔"),
    TRANSPORT("Transport", "🚕"),
    SHOPPING("Shopping", "🛍️"),
    STUDIES("Studies", "📚"),
    ENTERTAINMENT("Entertainment", "🎮"),
    OTHER("Other", "📦")
}

@Entity(tableName = "expenses")
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
