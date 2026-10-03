package com.example.ui.components.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseUrgent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskItemCard(
    task: TaskItem,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    val cardBorderColor by animateColorAsState(
        targetValue = if (task.isCompleted) EmeraldSuccess.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "cardBorderColor"
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "cardBgColor"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1.15f else 1.0f,
        animationSpec = tween(200),
        label = "checkScale"
    )

    val isDueToday = remember(task.dueDate) {
        val nowCal = Calendar.getInstance()
        val dueCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
        nowCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)
    }

    val isOverdue = remember(task.dueDate, task.isCompleted) {
        !task.isCompleted && task.dueDate < System.currentTimeMillis() - 86400000L
    }

    val formattedDueDate = remember(task.dueDate) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(task.dueDate))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleCompletion() }
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Checkbox / Toggle Status Button
                FilledIconToggleButton(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompletion() },
                    modifier = Modifier
                        .size(44.dp)
                        .scale(checkScale)
                        .testTag("task_check_${task.id}"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        checkedContainerColor = EmeraldSuccess,
                        checkedContentColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    if (task.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = strings.markIncomplete,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }

                // Title and Description
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (task.isCompleted) 0.5f else 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Edit & Delete actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = strings.edit,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata row: Category, Priority, Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category & Priority Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Badge
                    val catLabel = when (task.category) {
                        TaskCategory.STUDIES -> strings.categoryStudies
                        TaskCategory.WORK -> strings.categoryWork
                        TaskCategory.PERSONAL -> strings.categoryPersonal
                        TaskCategory.GOALS -> strings.categoryGoals
                    }
                    Surface(
                        color = IndigoPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${task.category.emoji} $catLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = IndigoPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Priority Badge
                    val (prioLabel, prioColor) = when (task.priority) {
                        TaskPriority.URGENT -> strings.urgent to RoseUrgent
                        TaskPriority.IMPORTANT -> strings.important to AmberAccent
                        TaskPriority.NORMAL -> strings.normal to EmeraldSuccess
                    }
                    Surface(
                        color = prioColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = task.priority.emoji, fontSize = 11.sp)
                            Text(
                                text = prioLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = prioColor
                            )
                        }
                    }
                }

                // Due Date Badge
                val dueColor = when {
                    task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    isOverdue -> RoseUrgent
                    isDueToday -> AmberAccent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = strings.dueDate,
                        tint = dueColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isDueToday && !task.isCompleted) strings.dueToday else "${strings.dueLabel} $formattedDueDate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isDueToday || isOverdue) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = dueColor
                    )
                }
            }
        }
    }
}
