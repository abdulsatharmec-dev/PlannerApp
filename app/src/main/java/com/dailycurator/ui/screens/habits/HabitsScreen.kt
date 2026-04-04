package com.dailycurator.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.ui.components.HabitCard
import com.dailycurator.ui.theme.*

@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // App bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Curator", style = MaterialTheme.typography.titleLarge.copy(color = Primary))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.GridView, contentDescription = null, tint = TextSecondary)
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PERFORMANCE TRACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary, letterSpacing = 1.sp))
                    Text("Habits",
                        style = MaterialTheme.typography.displayLarge.copy(color = TextPrimary))
                }
                Button(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Habit",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White, fontWeight = FontWeight.SemiBold))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // AI Habit Extractor card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F7FA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null,
                            tint = AccentTeal, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Habit Extractor",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
                        Text("Analyze your journal for new habits",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary,
                        modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── BUILDING MOMENTUM
        item {
            Text("BUILDING MOMENTUM",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary, letterSpacing = 1.5.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        items(state.buildingHabits, key = { it.id }) { habit ->
            HabitCard(
                habit = habit,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── ELIMINATING FRICTION
        item {
            Spacer(Modifier.height(8.dp))
            Text("ELIMINATING FRICTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentRed, letterSpacing = 1.5.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        items(state.eliminatingHabits, key = { it.id }) { habit ->
            HabitCard(
                habit = habit,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
