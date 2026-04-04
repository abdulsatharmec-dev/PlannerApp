package com.dailycurator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.ui.navigation.AppNavHost
import com.dailycurator.ui.theme.DailyCuratorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by prefs.darkThemeFlow.collectAsState(initial = prefs.isDarkTheme())
            DailyCuratorTheme(darkTheme = isDark) {
                AppNavHost()
            }
        }
    }
}

