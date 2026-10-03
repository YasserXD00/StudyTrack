package com.example.ui.components.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel that manages the Pomodoro-style study timer countdown state
 * and tracks overall session progress and cycle intervals.
 */
class PomodoroTimerViewModel : ViewModel() {

    private val _countdownState = MutableStateFlow(PomodoroCountdownState())
    val countdownState: StateFlow<PomodoroCountdownState> = _countdownState.asStateFlow()

    private val _sessionProgress = MutableStateFlow(PomodoroSessionProgress())
    val sessionProgress: StateFlow<PomodoroSessionProgress> = _sessionProgress.asStateFlow()

    private val _sessionEvents = MutableSharedFlow<PomodoroSessionEvent>(extraBufferCapacity = 1)
    val sessionEvents: SharedFlow<PomodoroSessionEvent> = _sessionEvents.asSharedFlow()

    private var timerJob: Job? = null

    // Callback that can be wired to database or parent ViewModel
    var onFocusSessionCompleted: ((durationMinutes: Int, subject: String) -> Unit)? = null

    fun startTimer() {
        if (_countdownState.value.isRunning) return
        _countdownState.update { it.copy(isRunning = true, isPaused = false) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_countdownState.value.remainingSeconds > 0 && _countdownState.value.isRunning) {
                delay(1000)
                _countdownState.update { current ->
                    current.copy(remainingSeconds = (current.remainingSeconds - 1).coerceAtLeast(0))
                }
            }
            if (_countdownState.value.remainingSeconds <= 0 && _countdownState.value.isRunning) {
                handleSessionComplete()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _countdownState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        val totalSecs = _countdownState.value.presetMinutes * 60
        _countdownState.update {
            it.copy(
                remainingSeconds = totalSecs,
                totalSeconds = totalSecs,
                isRunning = false,
                isPaused = false
            )
        }
    }

    fun skipTimer() {
        timerJob?.cancel()
        timerJob = null
        val currentMode = _countdownState.value.mode

        if (currentMode == PomodoroMode.FOCUS) {
            transitionToBreak(isCompleted = false)
        } else {
            transitionToFocus()
        }
    }

    fun selectMode(newMode: PomodoroMode) {
        timerJob?.cancel()
        timerJob = null
        val mins = newMode.defaultMinutes
        val totalSecs = mins * 60
        _countdownState.update {
            it.copy(
                mode = newMode,
                presetMinutes = mins,
                totalSeconds = totalSecs,
                remainingSeconds = totalSecs,
                isRunning = false,
                isPaused = false
            )
        }
    }

    fun selectPreset(focusMinutes: Int, breakMinutes: Int) {
        timerJob?.cancel()
        timerJob = null
        val totalSecs = focusMinutes * 60
        _countdownState.update {
            it.copy(
                mode = PomodoroMode.FOCUS,
                presetMinutes = focusMinutes,
                totalSeconds = totalSecs,
                remainingSeconds = totalSecs,
                isRunning = false,
                isPaused = false
            )
        }
    }

    fun setCustomDuration(minutes: Int) {
        val clampedMins = minutes.coerceIn(1, 180)
        timerJob?.cancel()
        timerJob = null
        val totalSecs = clampedMins * 60
        _countdownState.update {
            it.copy(
                mode = PomodoroMode.FOCUS,
                presetMinutes = clampedMins,
                totalSeconds = totalSecs,
                remainingSeconds = totalSecs,
                isRunning = false,
                isPaused = false
            )
        }
    }

    fun selectSubject(subject: String) {
        _countdownState.update { it.copy(selectedSubject = subject) }
    }

    fun setTargetSessions(count: Int) {
        _sessionProgress.update { it.copy(targetSessionsToday = count.coerceAtLeast(1)) }
    }

    fun toggleAutoStartBreak() {
        _sessionProgress.update { it.copy(autoStartBreak = !it.autoStartBreak) }
    }

    fun resetSessionProgressToZero() {
        _sessionProgress.update {
            it.copy(
                completedSessionsToday = 0,
                currentCycleStep = 1,
                totalFocusMinutesToday = 0
            )
        }
    }

    private fun handleSessionComplete() {
        timerJob?.cancel()
        timerJob = null

        val finishedState = _countdownState.value
        if (finishedState.mode == PomodoroMode.FOCUS) {
            val sessionMinutes = finishedState.presetMinutes
            val subject = finishedState.selectedSubject

            // Update session progress
            _sessionProgress.update { progress ->
                val newCount = progress.completedSessionsToday + 1
                val newTotalMins = progress.totalFocusMinutesToday + sessionMinutes
                val nextCycle = if (progress.currentCycleStep >= 4) 1 else progress.currentCycleStep + 1
                progress.copy(
                    completedSessionsToday = newCount,
                    totalFocusMinutesToday = newTotalMins,
                    currentCycleStep = nextCycle
                )
            }

            // Emit event and invoke callback
            val event = PomodoroSessionEvent.FocusCompleted(
                durationMinutes = sessionMinutes,
                subject = subject,
                xpEarned = 20
            )
            _sessionEvents.tryEmit(event)
            onFocusSessionCompleted?.invoke(sessionMinutes, subject)

            // Transition to break
            transitionToBreak(isCompleted = true)
        } else {
            val isLong = finishedState.mode == PomodoroMode.LONG_BREAK
            _sessionEvents.tryEmit(PomodoroSessionEvent.BreakCompleted(isLongBreak = isLong))
            transitionToFocus()
        }
    }

    private fun transitionToBreak(isCompleted: Boolean) {
        val currentCycle = _sessionProgress.value.currentCycleStep
        // Every 4 sessions, offer a Long Break (15 mins)
        val nextMode = if (currentCycle == 1 && isCompleted) PomodoroMode.LONG_BREAK else PomodoroMode.SHORT_BREAK
        val breakMins = if (nextMode == PomodoroMode.LONG_BREAK) 15 else 5
        val breakSecs = breakMins * 60

        _countdownState.update {
            it.copy(
                mode = nextMode,
                presetMinutes = breakMins,
                totalSeconds = breakSecs,
                remainingSeconds = breakSecs,
                isRunning = false,
                isPaused = false
            )
        }

        if (_sessionProgress.value.autoStartBreak) {
            startTimer()
        }
    }

    private fun transitionToFocus() {
        val focusMins = 25
        val focusSecs = focusMins * 60

        _countdownState.update {
            it.copy(
                mode = PomodoroMode.FOCUS,
                presetMinutes = focusMins,
                totalSeconds = focusSecs,
                remainingSeconds = focusSecs,
                isRunning = false,
                isPaused = false
            )
        }

        if (_sessionProgress.value.autoStartNextFocus) {
            startTimer()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
