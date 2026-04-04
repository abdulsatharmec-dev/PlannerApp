package com.dailycurator.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME  = "curator_prefs"
private const val KEY_DARK    = "dark_theme"

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Emits the current dark-theme value and re-emits whenever it changes. */
    val darkThemeFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DARK) trySend(prefs.getBoolean(KEY_DARK, false))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Emit initial value immediately
        trySend(prefs.getBoolean(KEY_DARK, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, enabled).apply()
    }

    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK, false)
}
