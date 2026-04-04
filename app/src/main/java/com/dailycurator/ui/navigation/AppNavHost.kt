package com.dailycurator.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailycurator.ui.screens.goals.GoalsScreen
import com.dailycurator.ui.screens.habits.HabitsScreen
import com.dailycurator.ui.screens.settings.SettingsScreen
import com.dailycurator.ui.screens.tasks.TasksScreen
import com.dailycurator.ui.screens.today.TodayScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Today.route)    { TodayScreen() }
            composable(Screen.Tasks.route)    { TasksScreen() }
            composable(Screen.Habits.route)   { HabitsScreen() }
            composable(Screen.Goals.route)    { GoalsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())
