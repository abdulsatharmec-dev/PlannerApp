package com.dailycurator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun DailyCuratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppThemePalette = AppThemePalette.VIOLET,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) palette.darkColorScheme() else palette.lightColorScheme()
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}
