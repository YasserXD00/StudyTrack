package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromTaskPriority(value: TaskPriority?): String? = value?.name

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority? =
        value?.let { runCatching { TaskPriority.valueOf(it) }.getOrDefault(TaskPriority.NORMAL) }

    @TypeConverter
    fun fromTaskCategory(value: TaskCategory?): String? = value?.name

    @TypeConverter
    fun toTaskCategory(value: String?): TaskCategory? =
        value?.let { runCatching { TaskCategory.valueOf(it) }.getOrDefault(TaskCategory.STUDIES) }

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun toExpenseCategory(value: String?): ExpenseCategory? =
        value?.let { runCatching { ExpenseCategory.valueOf(it) }.getOrDefault(ExpenseCategory.FOOD) }

    @TypeConverter
    fun fromBookStatus(value: BookStatus?): String? = value?.name

    @TypeConverter
    fun toBookStatus(value: String?): BookStatus? =
        value?.let { runCatching { BookStatus.valueOf(it) }.getOrDefault(BookStatus.TO_READ) }

    @TypeConverter
    fun fromEnglishSkill(value: EnglishSkill?): String? = value?.name

    @TypeConverter
    fun toEnglishSkill(value: String?): EnglishSkill? =
        value?.let { runCatching { EnglishSkill.valueOf(it) }.getOrDefault(EnglishSkill.VOCABULARY) }
}
