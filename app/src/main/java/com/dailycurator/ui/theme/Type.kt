package com.dailycurator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 28.sp, fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 17.sp, fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.Normal
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp
    )
)
