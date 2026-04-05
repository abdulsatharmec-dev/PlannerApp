package com.dailycurator.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitType
import com.dailycurator.ui.theme.AccentDeepGreen
import com.dailycurator.ui.theme.AccentGreen
import com.dailycurator.ui.theme.AccentRed
import com.dailycurator.ui.theme.AccentTeal
import com.dailycurator.ui.theme.Primary
import com.dailycurator.ui.theme.ProgressTrack

@Composable
fun HabitCard(
    habit: Habit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onQuickDone: () -> Unit = {},
) {
    val isEliminating = habit.habitType == HabitType.ELIMINATING
    val categoryColor = when (habit.category) {
        "Physical", "PHYSICAL" -> AccentTeal
        "Mental", "MENTAL" -> AccentRed
        "Spiritual", "SPIRITUAL" -> AccentDeepGreen
        else -> Primary
    }
    val progressColor = when {
        isEliminating -> AccentRed
        habit.isDone || habit.isGoalMet -> AccentDeepGreen
        else -> categoryColor
    }
    val trackColor = ProgressTrack
    val cardTint = when {
        habit.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        habit.progress >= 0.99f -> AccentGreen.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }

    var animStarted by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animStarted) habit.progress else 0f,
        animationSpec = tween(500),
        label = "habitProgress",
    )
    LaunchedEffect(Unit) { animStarted = true }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardTint),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .then(if (habit.isDone) Modifier.alpha(0.72f) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 4.dp,
                    color = progressColor,
                    trackColor = trackColor,
                    strokeCap = StrokeCap.Round,
                )
                Text(habit.iconEmoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                val valueStr = if (habit.unit.contains("L", ignoreCase = true)) {
                    "%.1f / %.1f %s".format(habit.currentValue, habit.targetValue, habit.unit)
                } else {
                    "${habit.currentValue.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}"
                }
                Text(
                    valueStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🔥 ${habit.streakDays}  ·  best ${habit.longestStreak}",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${habit.completionPercent}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = progressColor,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(trackColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(progressColor),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (!habit.isDone) {
                FilledIconButton(
                    onClick = onQuickDone,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Mark done", modifier = Modifier.size(22.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentDeepGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Done",
                        tint = AccentDeepGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
