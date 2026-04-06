package com.dailycurator.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Accent families: each defines both light and dark [ColorScheme]s.
 * [VIOLET] matches the original app palette.
 */
enum class AppThemePalette(
    val storageId: String,
    val displayLabel: String,
) {
    VIOLET(
        storageId = "violet",
        displayLabel = "Violet (default)",
    ),
    MIDNIGHT(
        storageId = "midnight",
        displayLabel = "Midnight black",
    ),
    FOREST(
        storageId = "forest",
        displayLabel = "Forest green",
    ),
    SUNSET(
        storageId = "sunset",
        displayLabel = "Sunset orange",
    ),
    CRIMSON(
        storageId = "crimson",
        displayLabel = "Crimson red",
    ),
    OCEAN(
        storageId = "ocean",
        displayLabel = "Ocean blue",
    ),
    ;

    fun lightColorScheme(): ColorScheme = when (this) {
        VIOLET -> lightColorScheme(
            primary = Primary,
            onPrimary = Surface,
            primaryContainer = PrimaryButton,
            onPrimaryContainer = Surface,
            secondary = AccentGreen,
            onSecondary = Color.White,
            tertiary = AccentTeal,
            background = Background,
            onBackground = TextPrimary,
            surface = Surface,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = Divider,
        )
        MIDNIGHT -> lightColorScheme(
            primary = Color(0xFF37474F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCFD8DC),
            onPrimaryContainer = Color(0xFF263238),
            secondary = Color(0xFF546E7A),
            onSecondary = Color.White,
            tertiary = Color(0xFF78909C),
            background = Color(0xFFF5F6F8),
            onBackground = Color(0xFF1C1C1E),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1C1C1E),
            surfaceVariant = Color(0xFFECEFF1),
            onSurfaceVariant = Color(0xFF5F6368),
            outline = Color(0xFFB0BEC5),
        )
        FOREST -> lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFC8E6C9),
            onPrimaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF00897B),
            onSecondary = Color.White,
            tertiary = Color(0xFF43A047),
            background = Color(0xFFE8F5E9),
            onBackground = Color(0xFF10291A),
            surface = Color(0xFFF1F8F4),
            onSurface = Color(0xFF10291A),
            surfaceVariant = Color(0xFFDCEFE0),
            onSurfaceVariant = Color(0xFF3D5345),
            outline = Color(0xFFA5D6A7),
        )
        SUNSET -> lightColorScheme(
            primary = Color(0xFFE65100),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE0B2),
            onPrimaryContainer = Color(0xFFBF360C),
            secondary = Color(0xFFFF6F00),
            onSecondary = Color.White,
            tertiary = Color(0xFFFFAB40),
            background = Color(0xFFFFF3E0),
            onBackground = Color(0xFF3E2723),
            surface = Color(0xFFFFF8F0),
            onSurface = Color(0xFF3E2723),
            surfaceVariant = Color(0xFFFFE0CC),
            onSurfaceVariant = Color(0xFF6D4C41),
            outline = Color(0xFFFFCC80),
        )
        CRIMSON -> lightColorScheme(
            primary = Color(0xFFC62828),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFCDD2),
            onPrimaryContainer = Color(0xFFB71C1C),
            secondary = Color(0xFFD32F2F),
            onSecondary = Color.White,
            tertiary = Color(0xFFEF5350),
            background = Color(0xFFFFEBEE),
            onBackground = Color(0xFF3E161C),
            surface = Color(0xFFFFF5F6),
            onSurface = Color(0xFF3E161C),
            surfaceVariant = Color(0xFFF8D7DA),
            onSurfaceVariant = Color(0xFF6D3B42),
            outline = Color(0xFFEF9A9A),
        )
        OCEAN -> lightColorScheme(
            primary = Color(0xFF0277BD),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB3E5FC),
            onPrimaryContainer = Color(0xFF01579B),
            secondary = Color(0xFF0097A7),
            onSecondary = Color.White,
            tertiary = Color(0xFF26C6DA),
            background = Color(0xFFE1F5FE),
            onBackground = Color(0xFF0D1B2A),
            surface = Color(0xFFF0F9FF),
            onSurface = Color(0xFF0D1B2A),
            surfaceVariant = Color(0xFFBEE3F8),
            onSurfaceVariant = Color(0xFF37474F),
            outline = Color(0xFF81D4FA),
        )
    }

    fun darkColorScheme(): ColorScheme = when (this) {
        VIOLET -> darkColorScheme(
            primary = DarkPrimary,
            onPrimary = DarkTextPrimary,
            primaryContainer = DarkPrimaryButton,
            onPrimaryContainer = DarkTextPrimary,
            secondary = AccentGreen,
            onSecondary = Color(0xFF0D1F14),
            tertiary = AccentTeal,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkDivider,
        )
        MIDNIGHT -> darkColorScheme(
            primary = Color(0xFF90A4AE),
            onPrimary = Color(0xFF0A0A0C),
            primaryContainer = Color(0xFF37474F),
            onPrimaryContainer = Color(0xFFECEFF1),
            secondary = Color(0xFFB0BEC5),
            onSecondary = Color(0xFF0A0A0C),
            tertiary = Color(0xFF78909C),
            background = Color(0xFF000000),
            onBackground = Color(0xFFE8EAED),
            surface = Color(0xFF0E0E10),
            onSurface = Color(0xFFE8EAED),
            surfaceVariant = Color(0xFF1A1A1D),
            onSurfaceVariant = Color(0xFF9AA0A6),
            outline = Color(0xFF3C4043),
        )
        FOREST -> darkColorScheme(
            primary = Color(0xFF81C784),
            onPrimary = Color(0xFF0D1F14),
            primaryContainer = Color(0xFF2E7D32),
            onPrimaryContainer = Color(0xFFE8F5E9),
            secondary = Color(0xFF4DB6AC),
            onSecondary = Color(0xFF002019),
            tertiary = Color(0xFFA5D6A7),
            background = Color(0xFF0D1A12),
            onBackground = Color(0xFFE8F5E9),
            surface = Color(0xFF121F18),
            onSurface = Color(0xFFE8F5E9),
            surfaceVariant = Color(0xFF1B2E22),
            onSurfaceVariant = Color(0xFFA5C9B0),
            outline = Color(0xFF355E4A),
        )
        SUNSET -> darkColorScheme(
            primary = Color(0xFFFFAB91),
            onPrimary = Color(0xFF2D1608),
            primaryContainer = Color(0xFFD84315),
            onPrimaryContainer = Color(0xFFFFE0D4),
            secondary = Color(0xFFFFCC80),
            onSecondary = Color(0xFF2D1608),
            tertiary = Color(0xFFFFB74D),
            background = Color(0xFF1A0F0C),
            onBackground = Color(0xFFFFE8E0),
            surface = Color(0xFF241812),
            onSurface = Color(0xFFFFE8E0),
            surfaceVariant = Color(0xFF332018),
            onSurfaceVariant = Color(0xFFD7B8A8),
            outline = Color(0xFF5D4037),
        )
        CRIMSON -> darkColorScheme(
            primary = Color(0xFFFF8A80),
            onPrimary = Color(0xFF2D0A0F),
            primaryContainer = Color(0xFFB71C1C),
            onPrimaryContainer = Color(0xFFFFEBEE),
            secondary = Color(0xFFFF5252),
            onSecondary = Color.White,
            tertiary = Color(0xFFFFCDD2),
            background = Color(0xFF140A0D),
            onBackground = Color(0xFFFFE4E6),
            surface = Color(0xFF1E1216),
            onSurface = Color(0xFFFFE4E6),
            surfaceVariant = Color(0xFF2D181E),
            onSurfaceVariant = Color(0xFFE0B4BC),
            outline = Color(0xFF5D2A35),
        )
        OCEAN -> darkColorScheme(
            primary = Color(0xFF81D4FA),
            onPrimary = Color(0xFF001E2F),
            primaryContainer = Color(0xFF0277BD),
            onPrimaryContainer = Color(0xFFE1F5FE),
            secondary = Color(0xFF4DD0E1),
            onSecondary = Color(0xFF002022),
            tertiary = Color(0xFFB3E5FC),
            background = Color(0xFF0A1628),
            onBackground = Color(0xFFE3F2FD),
            surface = Color(0xFF0F1E32),
            onSurface = Color(0xFFE3F2FD),
            surfaceVariant = Color(0xFF152A40),
            onSurfaceVariant = Color(0xFFB0BEC5),
            outline = Color(0xFF37474F),
        )
    }

    /** Swatch color for settings chips (matches current light/dark mode). */
    fun previewPrimary(forDarkMode: Boolean): Color =
        if (forDarkMode) darkColorScheme().primary else lightColorScheme().primary

    companion object {
        fun fromStorageId(id: String?): AppThemePalette {
            if (id.isNullOrBlank()) return VIOLET
            return entries.firstOrNull { it.storageId == id } ?: VIOLET
        }
    }
}
