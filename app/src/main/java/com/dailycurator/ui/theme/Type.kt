package com.dailycurator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal, color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Normal, color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.Normal, color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp, color = TextSecondary
    )
)
