package com.dailycurator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.ui.theme.AccentGreen

@Composable
fun GoalListItem(goal: WeeklyGoal, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (goal.isCompleted) AccentGreen else Color.Transparent)
                .border(
                    1.5.dp,
                    if (goal.isCompleted) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (goal.isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                goal.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (goal.isCompleted) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (goal.isCompleted) FontWeight.Medium else FontWeight.Normal
                )
            )
            val info = buildString {
                if (goal.category.isNotEmpty()) append(goal.category)
                if (goal.deadline != null) append(" • Due ${goal.deadline}")
                if (goal.timeEstimate != null) append(" • ${goal.timeEstimate}")
            }
            if (info.isNotBlank()) {
                Text(
                    info,
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}
