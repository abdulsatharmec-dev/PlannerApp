package com.dailycurator.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.ui.screens.chat.ChatScreen
import com.dailycurator.ui.screens.gmail.GmailMailboxSummaryScreen
import com.dailycurator.ui.screens.goals.GoalsScreen
import com.dailycurator.ui.screens.schedule.ScheduleScreen
import com.dailycurator.ui.screens.schedule.ScheduleTopAppBar
import com.dailycurator.ui.screens.schedule.ScheduleViewModel
import com.dailycurator.ui.screens.habits.HabitsScreen
import com.dailycurator.ui.screens.journal.JournalEditorScreen
import com.dailycurator.ui.screens.journal.JournalsScreen
import com.dailycurator.ui.screens.memory.MemoryManagementScreen
import com.dailycurator.ui.screens.phoneusage.PhoneUsageScreen
import com.dailycurator.ui.screens.pomodoro.PomodoroScreen
import com.dailycurator.ui.screens.settings.SettingsScreen
import com.dailycurator.ui.screens.tasks.TasksScreen
import com.dailycurator.ui.screens.today.TodayScreen
import com.dailycurator.ui.theme.appScaffoldContainerColor
import com.dailycurator.R
import kotlinx.coroutines.launch

private fun subPageTitle(route: String?): String? = when (route) {
    Screen.Settings.route -> "Settings"
    Screen.Journal.route -> "Journal"
    Screen.GmailMailboxSummary.route -> "Gmail summary"
    Screen.AgentMemory.route -> "AI memory"
    Screen.PhoneUsage.route -> "Phone usage"
    else -> null
}

@Composable
private fun mainScreenTopBarTitle(currentRoute: String?): String {
    bottomNavItems.find { it.route == currentRoute }?.label?.let { return it }
    subPageTitle(currentRoute)?.let { return it }
    val route = currentRoute ?: return stringResource(R.string.app_name)
    return route.replaceFirstChar { it.uppercaseChar() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    openPomodoroRequest: Boolean = false,
    openHabitsRequest: Boolean = false,
    onConsumedOpenPomodoro: () -> Unit = {},
    onConsumedOpenHabits: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    LaunchedEffect(openPomodoroRequest) {
        if (openPomodoroRequest) {
            navController.navigate(Screen.Pomodoro.route) { launchSingleTop = true }
            onConsumedOpenPomodoro()
        }
    }
    LaunchedEffect(openHabitsRequest) {
        if (openHabitsRequest) {
            navController.navigate(Screen.Habits.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
            onConsumedOpenHabits()
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isJournalEditor = currentRoute?.startsWith("journal_editor/") == true
    val journalDrawerSelected =
        currentRoute == Screen.Journal.route || isJournalEditor
    val pomodoroDrawerSelected = currentRoute == Screen.Pomodoro.route
    val gmailDrawerSelected = currentRoute == Screen.GmailMailboxSummary.route
    val memoryDrawerSelected = currentRoute == Screen.AgentMemory.route
    val phoneUsageDrawerSelected = currentRoute == Screen.PhoneUsage.route
    val goalsDrawerSelected = currentRoute == Screen.Goals.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Screen.Goals.icon, contentDescription = null) },
                    label = { Text("Goals") },
                    selected = goalsDrawerSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Goals.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                NavigationDrawerItem(
                    icon = { Icon(Screen.Journal.icon, contentDescription = null) },
                    label = { Text("Journal") },
                    selected = journalDrawerSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (isJournalEditor) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Journal.route) { launchSingleTop = true }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                NavigationDrawerItem(
                    icon = { Icon(Screen.Pomodoro.icon, contentDescription = null) },
                    label = { Text("Pomodoro") },
                    selected = pomodoroDrawerSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Pomodoro.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                NavigationDrawerItem(
                    icon = { Icon(Screen.GmailMailboxSummary.icon, contentDescription = null) },
                    label = { Text("Gmail summary") },
                    selected = gmailDrawerSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.GmailMailboxSummary.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                NavigationDrawerItem(
                    icon = { Icon(Screen.AgentMemory.icon, contentDescription = null) },
                    label = { Text("AI memory") },
                    selected = memoryDrawerSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.AgentMemory.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                NavigationDrawerItem(
                    icon = { Icon(Screen.PhoneUsage.icon, contentDescription = null) },
                    label = { Text("Phone usage") },
                    selected = phoneUsageDrawerSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.PhoneUsage.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        },
    ) {
        Scaffold(
            containerColor = appScaffoldContainerColor(),
            topBar = {
                if (!isJournalEditor) {
                    when {
                        currentRoute == Screen.Schedule.route -> {
                            val entry = navController.currentBackStackEntry
                            if (entry != null) {
                                val scheduleVm: ScheduleViewModel = hiltViewModel(entry)
                                ScheduleTopAppBar(
                                    viewModel = scheduleVm,
                                    onOpenMenu = { scope.launch { drawerState.open() } },
                                    onNavigateSettings = { navController.navigate(Screen.Settings.route) },
                                )
                            } else {
                                CenterAlignedTopAppBar(
                                    title = { Text("Schedule", style = MaterialTheme.typography.titleMedium) },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = appScaffoldContainerColor(),
                                    ),
                                )
                            }
                        }
                        currentRoute == Screen.Tasks.route -> {
                            // TasksScreen provides its own title row + actions; skip duplicate top app bar.
                        }
                        subPageTitle(currentRoute) != null -> {
                            TopAppBar(
                                title = {
                                    Text(
                                        subPageTitle(currentRoute)!!,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = appScaffoldContainerColor(),
                                ),
                            )
                        }
                        else -> {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        mainScreenTopBarTitle(currentRoute),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = appScaffoldContainerColor(),
                                ),
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (isJournalEditor) {
                    Spacer(Modifier.height(0.dp))
                } else {
                    val density = LocalDensity.current
                    val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
                    val hideBottomNav = currentRoute == Screen.Chat.route && keyboardOpen
                    if (hideBottomNav) {
                        Spacer(Modifier.height(0.dp))
                    } else {
                        NavigationBar(
                            containerColor = appScaffoldContainerColor(),
                            tonalElevation = 0.dp,
                        ) {
                            bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    },
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Today.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Today.route) {
                    TodayScreen(
                        onNavigateToPomodoro = { navController.navigate(Screen.Pomodoro.route) },
                        onOpenGmailMailboxSummary = {
                            navController.navigate(Screen.GmailMailboxSummary.route) { launchSingleTop = true }
                        },
                    )
                }
                composable(Screen.Schedule.route) { ScheduleScreen() }
                composable(Screen.Tasks.route) {
                    TasksScreen(
                        onNavigateToPomodoro = { navController.navigate(Screen.Pomodoro.route) },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateSettings = { navController.navigate(Screen.Settings.route) },
                    )
                }
                composable(Screen.Habits.route) {
                    HabitsScreen(
                        onNavigateToPomodoro = { navController.navigate(Screen.Pomodoro.route) },
                    )
                }
                composable(Screen.Goals.route) {
                    GoalsScreen(
                        onNavigateToPomodoro = { navController.navigate(Screen.Pomodoro.route) },
                    )
                }
                composable(Screen.Pomodoro.route) { PomodoroScreen() }
                composable(Screen.Chat.route) { ChatScreen() }
                composable(Screen.Journal.route) {
                    JournalsScreen(
                        onCreateEntry = { navController.navigate(journalEditorRoute(0L)) },
                        onOpenEntry = { id -> navController.navigate(journalEditorRoute(id)) },
                    )
                }
                composable(
                    route = JOURNAL_EDITOR_ROUTE,
                    arguments = listOf(
                        navArgument("entryId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
                ) {
                    JournalEditorScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(Screen.GmailMailboxSummary.route) { GmailMailboxSummaryScreen() }
                composable(Screen.AgentMemory.route) { MemoryManagementScreen() }
                composable(Screen.PhoneUsage.route) { PhoneUsageScreen() }
            }
        }
    }
}
