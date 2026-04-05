package com.dailycurator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material.icons.filled.List

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Today   : Screen("today",    "Timeline", Icons.Default.Home)
    object Tasks   : Screen("tasks",    "Tasks",    Icons.Default.List)
    object Habits  : Screen("habits",   "Habits",   Icons.Default.TrackChanges)
    object Goals   : Screen("goals",    "Goals",    Icons.Default.CheckCircle)
    object Chat    : Screen("chat",     "AI Agent", Icons.Default.AutoAwesome)
    object Journal : Screen("journal",  "Journal",  Icons.Default.MenuBook)
    object Pomodoro: Screen("pomodoro", "Pomodoro", Icons.Default.Timer)
    object Settings: Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.Today, Screen.Tasks, Screen.Habits, Screen.Goals, Screen.Chat)

const val JOURNAL_EDITOR_ROUTE = "journal_editor/{entryId}"

fun journalEditorRoute(entryId: Long): String = "journal_editor/$entryId"
