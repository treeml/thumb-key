package com.nightshift.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// True dark palette — near-black blue-greys, not dimmed light colours.
val Ink = Color(0xFF0B0E14) // window background
val Surface1 = Color(0xFF131722) // cards
val Surface2 = Color(0xFF1B2130) // raised elements
val Outline = Color(0xFF2A3245)
val TextPrimary = Color(0xFFE7EBF4)
val TextSecondary = Color(0xFF9AA4B8)
val Accent = Color(0xFF7EB6FF) // single restrained accent
val AccentDim = Color(0xFF3B5A85)

// Priority colours, tuned for a dark screen at 4 am.
val UrgentRed = Color(0xFFFF6B6B)
val SoonYellow = Color(0xFFFFD166)
val RoutineGreen = Color(0xFF6BCB77)
val DangerRed = Color(0xFFE5484D)

fun priorityColor(priority: Int): Color =
    when (priority) {
        1 -> UrgentRed
        2 -> SoonYellow
        else -> RoutineGreen
    }

fun priorityLabel(priority: Int): String =
    when (priority) {
        1 -> "URGENT"
        2 -> "SOON"
        else -> "ROUTINE"
    }

private val DarkColors =
    darkColorScheme(
        primary = Accent,
        onPrimary = Ink,
        secondary = AccentDim,
        background = Ink,
        onBackground = TextPrimary,
        surface = Surface1,
        onSurface = TextPrimary,
        surfaceVariant = Surface2,
        onSurfaceVariant = TextSecondary,
        outline = Outline,
        error = DangerRed,
    )

private val NightTypography =
    Typography(
        headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.2).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp),
    )

@Composable
fun NightshiftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = NightTypography,
        content = content,
    )
}
