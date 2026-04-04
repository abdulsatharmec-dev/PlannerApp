package com.dailycurator.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.ui.components.*
import com.dailycurator.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Daily Curator",
                    style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI",
                    tint = Primary, modifier = Modifier.size(22.dp))
            }
        }

        // ── AI Insight Card
        item {
            AIInsightCard(
                insight = state.insight,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Top 5 Priorities header
        item {
            Text("Top 5 Priorities",
                style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        // ── Priority items
        items(state.tasks, key = { it.id }) { task ->
            PriorityItem(
                task = task,
                onToggleDone = { viewModel.toggleTaskDone(task) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── New Task button
        item {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { /* TODO: open AddTaskSheet */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("New Task",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Weekly Goals section
        item {
            WeeklyGoalsSection(
                goals = state.goals,
                collapsed = state.goalsCollapsed,
                onToggleCollapse = viewModel::toggleGoalsCollapsed,
                onToggleGoal = viewModel::toggleGoal,
                insight = state.insight
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Today's Schedule
        item {
            TodayScheduleSection(
                events = state.scheduleEvents,
                activeTab = state.scheduleTab,
                onTabChange = viewModel::setScheduleTab,
                dateLabel = todayLabel
            )
        }
    }
}

@Composable
private fun WeeklyGoalsSection(
    goals: List<com.dailycurator.data.model.WeeklyGoal>,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleGoal: (com.dailycurator.data.model.WeeklyGoal) -> Unit,
    insight: com.dailycurator.data.model.AiInsight
) {
    val completedCount = goals.count { it.isCompleted }
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                tint = Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Weekly Goals",
                    style = MaterialTheme.typography.titleLarge.copy(color = Primary))
                Text("PROGRESS: $completedCount/${goals.size} GOALS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AccentGreen, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp))
            }
            IconButton(onClick = onToggleCollapse) {
                Icon(
                    if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Toggle", tint = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // AI Goal Insight
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(modifier = Modifier
                    .width(3.dp).fillMaxHeight()
                    .background(AccentGreen, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(AccentGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                                tint = AccentGreen, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            buildString {
                                append("AI Insight: Slight delay on Strategy Audit due to unplanned syncs this morning.")
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                    }
                    if (insight.recoveryPlan != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("Recovery plan: ${insight.recoveryPlan}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Normal))
                    }
                }
            }
        }

        // Goals list (animated collapse)
        AnimatedVisibility(visible = !collapsed) {
            Column {
                Spacer(Modifier.height(4.dp))
                goals.forEach { goal ->
                    GoalListItem(goal = goal, onToggle = { onToggleGoal(goal) })
                }
            }
        }
    }
}

@Composable
private fun TodayScheduleSection(
    events: List<com.dailycurator.data.model.ScheduleEvent>,
    activeTab: ScheduleTab,
    onTabChange: (ScheduleTab) -> Unit,
    dateLabel: String
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("TODAY",
            style = MaterialTheme.typography.labelSmall.copy(
                color = AccentGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Schedule",
                style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary),
                modifier = Modifier.weight(1f))
            Text(dateLabel,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
        }
        Spacer(Modifier.height(10.dp))

        // Tab chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleTab.values().forEach { tab ->
                val isActive = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) Primary else SurfaceVariant)
                        .clickable { onTabChange(tab) }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isActive) Color.White else TextSecondary,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal))
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Timeline or Clock
        Crossfade(targetState = activeTab, label = "schedule_tab") { tab ->
            when (tab) {
                ScheduleTab.TIMELINE -> TimelineView(events = events)
                ScheduleTab.CLOCK    -> ClockView(events = events)
            }
        }
    }
}

@Composable
private fun TimelineView(events: List<com.dailycurator.data.model.ScheduleEvent>) {
    val now = remember { LocalTime.now() }
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        events.forEach { event ->
            val isNowBefore = now.isAfter(event.startTime) && now.isBefore(event.endTime)
            val isNowAtStart = now.isBefore(event.startTime) &&
                    (events.indexOf(event) == 0 ||
                     now.isAfter(events[events.indexOf(event) - 1].endTime))

            if (isNowAtStart || isNowBefore) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(now.format(fmt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NowRed, fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(48.dp))
                    Spacer(Modifier.width(8.dp))
                    NowIndicator(modifier = Modifier.weight(1f))
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(event.startTime.format(fmt),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
                    modifier = Modifier.width(48.dp).padding(top = 10.dp))
                Spacer(Modifier.width(8.dp))
                TimelineEventCard(event = event, modifier = Modifier.weight(1f))
            }
        }
    }
}
