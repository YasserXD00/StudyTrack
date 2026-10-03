package com.example.ui.components.pomodoro

enum class PomodoroMode(val title: String, val emoji: String, val defaultMinutes: Int) {
    FOCUS("Focus", "🎯", 25),
    SHORT_BREAK("Short Break", "☕", 5),
    LONG_BREAK("Long Break", "🌴", 15)
}

data class PomodoroCountdownState(
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val presetMinutes: Int = 25,
    val selectedSubject: String = "Computer Science"
) {
    val progressRatio: Float
        get() = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f) else 0f

    val formattedTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

data class PomodoroSessionProgress(
    val completedSessionsToday: Int = 0,
    val currentCycleStep: Int = 1, // 1 to 4
    val targetSessionsToday: Int = 4,
    val totalFocusMinutesToday: Int = 0,
    val autoStartBreak: Boolean = false,
    val autoStartNextFocus: Boolean = false
) {
    val progressRatio: Float
        get() = if (targetSessionsToday > 0) {
            (completedSessionsToday.toFloat() / targetSessionsToday).coerceIn(0f, 1f)
        } else 0f
}

sealed class PomodoroSessionEvent {
    data class FocusCompleted(val durationMinutes: Int, val subject: String, val xpEarned: Int = 20) : PomodoroSessionEvent()
    data class BreakCompleted(val isLongBreak: Boolean) : PomodoroSessionEvent()
}
