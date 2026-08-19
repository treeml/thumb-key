package com.nightshift.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nightshift.tracker.ui.settings.AppSettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nightshift.tracker.BuildConfig

// Two palettes, chosen at compile time by flavor:
//  - Nightshift: true dark, near-black blue-greys for a 4 am corridor.
//  - UroDay: light "paper" theme with a teal accent for daylit wards.
private val URO = BuildConfig.URO

val Ink = if (URO) Color(0xFFF6F7F9) else Color(0xFF0B0E14) // window background
val Surface1 = if (URO) Color(0xFFFFFFFF) else Color(0xFF131722) // cards
val Surface2 = if (URO) Color(0xFFECEFF3) else Color(0xFF1B2130) // raised elements
val Outline = if (URO) Color(0xFFDDE2EA) else Color(0xFF2A3245)
val TextPrimary = if (URO) Color(0xFF171B26) else Color(0xFFE7EBF4)
val TextSecondary = if (URO) Color(0xFF5B6474) else Color(0xFF9AA4B8)
val Accent = if (URO) Color(0xFF0F766E) else Color(0xFF7EB6FF) // single restrained accent
val AccentDim = if (URO) Color(0xFF99C7C2) else Color(0xFF3B5A85)

// Priority colours, tuned per background.
val UrgentRed = if (URO) Color(0xFFD92D20) else Color(0xFFFF6B6B)
val SoonYellow = if (URO) Color(0xFFB45309) else Color(0xFFFFD166)
val RoutineGreen = if (URO) Color(0xFF15803D) else Color(0xFF6BCB77)
val DangerRed = if (URO) Color(0xFFB42318) else Color(0xFFE5484D)

// Body text on tinted info cards (cheat sheets, guides).
val CardBody = if (URO) Color(0xFF394150) else Color(0xFFD5DBE8)
val DangerBody = if (URO) Color(0xFF7A271A) else Color(0xFFF0C6C8)

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

private val LightColors =
    lightColorScheme(
        primary = Accent,
        onPrimary = Color.White,
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

/**
 * One type ramp, optionally scaled up for tired eyes at 4 am (Settings ->
 * Larger text). Scaling the ramp rather than the density keeps layouts intact.
 */
private fun typographyFor(scale: Float) =
    Typography(
        headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp * scale, letterSpacing = (-0.3).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp * scale, letterSpacing = (-0.2).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp * scale),
        bodyLarge = TextStyle(fontSize = 16.sp * scale, lineHeight = 23.sp * scale),
        bodyMedium = TextStyle(fontSize = 14.sp * scale, lineHeight = 20.sp * scale),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp * scale, letterSpacing = 0.2.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp * scale, letterSpacing = 0.8.sp),
    )

@Composable
fun NightshiftTheme(content: @Composable () -> Unit) {
    val large by AppSettings.largeText.collectAsStateWithLifecycle()
    MaterialTheme(
        colorScheme = if (URO) LightColors else DarkColors,
        typography = typographyFor(if (large) 1.15f else 1f),
        content = content,
    )
}
