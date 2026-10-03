package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EnglishSkill(val label: String, val emoji: String) {
    SPEAKING("Speaking", "🗣️"),
    VOCABULARY("Vocabulary", "📖"),
    GRAMMAR("Grammar", "✍️"),
    LISTENING("Listening", "👂"),
    READING("Reading", "📚")
}

@Entity(tableName = "vocabulary")
data class VocabularyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val meaning: String,
    val example: String = "",
    val skill: EnglishSkill = EnglishSkill.VOCABULARY,
    val isMastered: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
