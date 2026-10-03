package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BookStatus(val label: String, val emoji: String) {
    TO_READ("To Read", "📚"),
    READING("Reading", "📖"),
    FINISHED("Finished", "✅")
}

@Entity(tableName = "books")
data class BookItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val totalPages: Int,
    val pagesRead: Int = 0,
    val rating: Int = 0, // 0 to 5
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val status: BookStatus = BookStatus.TO_READ
) {
    val progressPercent: Int
        get() = if (totalPages > 0) ((pagesRead.toFloat() / totalPages) * 100).toInt().coerceIn(0, 100) else 0
}
