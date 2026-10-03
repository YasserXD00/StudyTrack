package com.example.ui.components.pomodoro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StudyTrackCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PomodoroStudyTimerComponent(
    timerViewModel: PomodoroTimerViewModel,
    modifier: Modifier = Modifier,
    onSessionCompletedCallback: ((durationMinutes: Int, subject: String) -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    // Wire callback if provided
    LaunchedEffect(timerViewModel, onSessionCompletedCallback) {
        if (onSessionCompletedCallback != null) {
            timerViewModel.onFocusSessionCompleted = onSessionCompletedCallback
        }
    }

    val countdownState by timerViewModel.countdownState.collectAsState()
    val sessionProgress by timerViewModel.sessionProgress.collectAsState()

    var showCustomDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }

    val defaultSubjects = listOf("Computer Science", "Calculus", "Linear Algebra", "English", "Physics", "Biology")

    val activePrimaryColor by animateColorAsState(
        targetValue = when (countdownState.mode) {
            PomodoroMode.FOCUS -> IndigoPrimary
            PomodoroMode.SHORT_BREAK -> EmeraldSuccess
            PomodoroMode.LONG_BREAK -> AmberAccent
        },
        animationSpec = tween(400),
        label = "activeColor"
    )

    val activeSecondaryColor by animateColorAsState(
        targetValue = when (countdownState.mode) {
            PomodoroMode.FOCUS -> CyanAccent
            PomodoroMode.SHORT_BREAK -> Color(0xFF34D399) // Mint
            PomodoroMode.LONG_BREAK -> Color(0xFFFBBF24) // Light amber
        },
        animationSpec = tween(400),
        label = "activeSecondaryColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = countdownState.progressRatio,
        animationSpec = tween(durationMillis = 350),
        label = "timerProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pomodoro_study_timer_component"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector Tabs (Focus / Short Break / Long Break)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PomodoroMode.values().forEach { mode ->
                    val isSelected = countdownState.mode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { timerViewModel.selectMode(mode) }
                            .testTag("pomodoro_mode_${mode.name.lowercase()}"),
                        color = if (isSelected) activePrimaryColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = mode.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (mode) {
                                    PomodoroMode.FOCUS -> strings.focusSession
                                    PomodoroMode.SHORT_BREAK -> strings.shortBreak
                                    PomodoroMode.LONG_BREAK -> strings.longBreak
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // Subject Selector (Active in Focus mode)
        if (countdownState.mode == PomodoroMode.FOCUS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.subject}:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = activePrimaryColor.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { showSubjectDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📚 ${countdownState.selectedSubject}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = activePrimaryColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select subject",
                            tint = activePrimaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Circular Countdown Progress Display
        Box(
            modifier = Modifier
                .size(240.dp)
                .testTag("pomodoro_countdown_circle"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(diameter, diameter)

                // Background Track
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Dynamic Animated Progress Arc
                val sweepAngle = 360f * animatedProgress
                if (sweepAngle > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(activePrimaryColor, activeSecondaryColor, activePrimaryColor),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Glowing tip indicator
                    val angleRad = Math.toRadians((-90f + sweepAngle).toDouble())
                    val radius = diameter / 2
                    val dotCenterX = center.x + (radius * cos(angleRad)).toFloat()
                    val dotCenterY = center.y + (radius * sin(angleRad)).toFloat()

                    drawCircle(
                        color = Color.White,
                        radius = (strokeWidth / 2.8f),
                        center = Offset(dotCenterX, dotCenterY)
                    )
                }
            }

            // Center Content: Time, Mode Badge, and Cycle Dots
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Cycle indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val isFilled = i < sessionProgress.currentCycleStep
                        val isCurrent = i == sessionProgress.currentCycleStep
                        Surface(
                            modifier = Modifier.size(if (isCurrent) 10.dp else 8.dp),
                            shape = CircleShape,
                            color = when {
                                isFilled -> activePrimaryColor
                                isCurrent -> activeSecondaryColor
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = countdownState.formattedTime,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("pomodoro_time_display")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = activePrimaryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (countdownState.mode) {
                            PomodoroMode.FOCUS -> if (countdownState.isRunning) "🔥 In Focus" else "🎯 Ready to Focus"
                            PomodoroMode.SHORT_BREAK -> "☕ Short Rest"
                            PomodoroMode.LONG_BREAK -> "🌴 Long Recharge"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = activePrimaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Primary Controls Row (Reset, Play/Pause/Resume, Skip)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            IconButton(
                onClick = { timerViewModel.resetTimer() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .testTag("pomodoro_reset_button")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = strings.resetTimer,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Main Play/Pause Button
            FilledIconButton(
                onClick = {
                    if (countdownState.isRunning) {
                        timerViewModel.pauseTimer()
                    } else if (countdownState.isPaused) {
                        timerViewModel.resumeTimer()
                    } else {
                        timerViewModel.startTimer()
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .testTag("pomodoro_main_toggle_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = activePrimaryColor
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (countdownState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (countdownState.isRunning) strings.pauseTimer else strings.startTimer,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Skip Button
            IconButton(
                onClick = { timerViewModel.skipTimer() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .testTag("pomodoro_skip_button")
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = strings.skipTimer,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Session Progress Card
        StudyTrackCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🍅", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Session Progress",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "${sessionProgress.completedSessionsToday} / ${sessionProgress.targetSessionsToday} completed",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = IndigoPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { sessionProgress.progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = activePrimaryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Focus Today: ${sessionProgress.totalFocusMinutesToday}m (+${sessionProgress.completedSessionsToday * 20} XP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = { timerViewModel.resetSessionProgressToZero() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Reset to 0", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Preset Quick Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { timerViewModel.selectPreset(25, 5) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("preset_25_5"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (countdownState.presetMinutes == 25 && countdownState.mode == PomodoroMode.FOCUS) {
                        IndigoPrimary.copy(alpha = 0.1f)
                    } else Color.Transparent
                )
            ) {
                Text("25 / 5m")
            }

            OutlinedButton(
                onClick = { timerViewModel.selectPreset(50, 10) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("preset_50_10"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (countdownState.presetMinutes == 50 && countdownState.mode == PomodoroMode.FOCUS) {
                        IndigoPrimary.copy(alpha = 0.1f)
                    } else Color.Transparent
                )
            ) {
                Text("50 / 10m")
            }

            OutlinedButton(
                onClick = { showCustomDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("preset_custom"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Custom ⏱️")
            }
        }
    }

    // Custom Duration Dialog
    if (showCustomDialog) {
        var customMinsInput by remember { mutableStateOf(countdownState.presetMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Set Custom Focus Duration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter focus duration in minutes (e.g. 15, 30, 45, 90):", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = customMinsInput,
                        onValueChange = { customMinsInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customMinsInput.toIntOrNull() ?: 25
                        timerViewModel.setCustomDuration(mins)
                        showCustomDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Subject Selection Dialog
    if (showSubjectDialog) {
        var customSubjectInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSubjectDialog = false },
            title = { Text("Select Study Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    defaultSubjects.forEach { subj ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    timerViewModel.selectSubject(subj)
                                    showSubjectDialog = false
                                },
                            color = if (countdownState.selectedSubject == subj) activePrimaryColor.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Text(
                                text = subj,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (countdownState.selectedSubject == subj) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customSubjectInput,
                        onValueChange = { customSubjectInput = it },
                        label = { Text("Or enter custom subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customSubjectInput.isNotBlank()) {
                            timerViewModel.selectSubject(customSubjectInput.trim())
                        }
                        showSubjectDialog = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
