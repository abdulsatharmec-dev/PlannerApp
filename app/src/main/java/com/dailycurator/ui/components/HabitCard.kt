package com.dailycurator.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitCategory
import com.dailycurator.data.model.HabitType
import com.dailycurator.ui.theme.*

@Composable
fun HabitCard(habit: Habit, modifier: Modifier = Modifier) {
    val isEliminating = habit.habitType == HabitType.ELIMINATING
    val categoryColor = when (habit.category) {
        HabitCategory.PHYSICAL  -> AccentTeal
        HabitCategory.MENTAL    -> AccentBrown
        HabitCategory.SPIRITUAL -> AccentDeepGreen
    }
    val progressColor = when {
        isEliminating -> AccentRed
        habit.category == HabitCategory.MENTAL    -> AccentRed
        habit.category == HabitCategory.SPIRITUAL -> AccentDeepGreen
        else -> Primary
    }
    val iconBgColor = when (habit.category) {
        HabitCategory.PHYSICAL  -> Color(0xFFE0F7FA)
        HabitCategory.MENTAL    -> Color(0xFFFBE9E7)
        HabitCategory.SPIRITUAL -> Color(0xFFE8F5E9)
    }

    var animStarted by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animStarted) habit.progress else 0f,
        animationSpec = tween(600), label = "progress"
    )
    LaunchedEffect(Unit) { animStarted = true }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(habit.iconEmoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(habit.name,
                        style = MaterialTheme.typography.titleLarge.copy(color = Primary),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        habit.category.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = categoryColor, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isEliminating) "Resistance" else "Streak",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${habit.streakDays} Days",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = if (isEliminating) AccentRed else Primary,
                            fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            // Progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val valueStr = if (habit.unit.contains("L"))
                    "%.1fL / %.1fL".format(habit.currentValue, habit.targetValue)
                else
                    "${habit.currentValue.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}"
                Text(valueStr, style = MaterialTheme.typography.bodySmall)
                if (habit.isGoalMet) {
                    Text("Goal Met",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AccentDeepGreen, fontWeight = FontWeight.SemiBold))
                } else {
                    Text("${(habit.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ProgressTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (habit.isGoalMet) AccentDeepGreen else progressColor)
                )
            }
            Spacer(Modifier.height(6.dp))
            // Bottom note
            val note = when {
                habit.isGoalMet -> "Completed for today"
                isEliminating   -> "${(habit.targetValue - habit.currentValue).toInt()} ${habit.unit.replace("limit","").trim()} remaining allowance"
                habit.unit.contains("L") -> "%.1fL remaining to goal".format(habit.targetValue - habit.currentValue)
                else -> "${(habit.targetValue - habit.currentValue).toInt()} ${habit.unit} remaining to goal"
            }
            Text(note,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (habit.isGoalMet) AccentDeepGreen else TextTertiary))
        }
    }
}
