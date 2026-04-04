package com.dailycurator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Today   : Screen("today",    "Timeline", Icons.Default.Home)
    object Habits  : Screen("habits",   "Habits",   Icons.Default.TrackChanges)
    object Goals   : Screen("goals",    "Goals",    Icons.Default.CheckCircle)
    object Settings: Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.Today, Screen.Habits, Screen.Goals, Screen.Settings)
