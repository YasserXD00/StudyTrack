package com.example.ui.i18n

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "English", "English", "🇺🇸", false),
    ARABIC("ar", "Arabic", "العربية", "🇸🇦", true),
    FRENCH("fr", "French", "Français", "🇫🇷", false),
    RUSSIAN("ru", "Russian", "Русский", "🇷🇺", false),
    GERMAN("de", "German", "Deutsch", "🇩🇪", false),
    CHINESE("zh", "Chinese", "中文", "🇨🇳", false),
    JAPANESE("ja", "Japanese", "日本語", "🇯🇵", false),
    SPANISH("es", "Spanish", "Español", "🇪🇸", false);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
