package com.nightshift.tracker.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.settings.AppSettings
import com.nightshift.tracker.ui.settings.leftHanded
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary

/**
 * One spacing scale, one radius scale, one set of controls.
 *
 * Before this existed every button was hand-rolled from a Box + background +
 * border, which is why the screens read as busy: a dozen outlined rectangles
 * competing for attention. Here, an outline means "this is a container" and
 * colour means "this is a state" — nothing else earns either.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object Radius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
}

/** Minimum comfortable one-handed target; bigger than the 48dp floor. */
val TouchTarget = 52.dp

/** Haptic tick on consequential taps — confirmation without looking. */
@Composable
fun rememberTick(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return {
        if (AppSettings.haptics.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

/** The standard card. No border by default — separation comes from surface. */
@Composable
fun NsCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(Radius.lg))
            .then(
                if (accent != null) {
                    Modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(Radius.lg))
                } else {
                    Modifier
                },
            ),
        content = content,
    )
}

/**
 * An action. [tone] carries the meaning: null = quiet, a colour = stateful.
 * Filled actions are reserved for the one primary action on a surface.
 */
@Composable
fun NsAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Color? = null,
    filled: Boolean = false,
    enabled: Boolean = true,
    haptic: Boolean = true,
) {
    val tick = rememberTick()
    val color = (tone ?: TextSecondary).let { if (enabled) it else it.copy(alpha = 0.4f) }
    Box(
        modifier
            .defaultMinSize(minHeight = TouchTarget)
            .background(
                if (filled) color.copy(alpha = 0.18f) else Surface2,
                RoundedCornerShape(Radius.md),
            ).then(
                if (filled) Modifier.border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(Radius.md)) else Modifier,
            ).clickable(enabled = enabled) {
                if (haptic) tick()
                onClick()
            }.padding(horizontal = Space.md, vertical = Space.md),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

/** Small non-interactive status pill. */
@Composable
fun NsChip(
    label: String,
    tone: Color = TextSecondary,
    strong: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(tone.copy(alpha = if (strong) 0.20f else 0.10f), RoundedCornerShape(Radius.sm))
            .then(if (strong) Modifier.border(1.dp, tone.copy(alpha = 0.5f), RoundedCornerShape(Radius.sm)) else Modifier)
            .padding(horizontal = Space.sm, vertical = Space.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = tone)
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color = TextSecondary,
) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = tone, modifier = modifier)
}

/**
 * Lays out a destructive action and a primary action so the primary one always
 * falls under the user's thumb and the destructive one never does.
 *
 * Right-handed: [secondary] … [primary].  Left-handed: [primary] … [secondary].
 */
@Composable
fun HandedActions(
    modifier: Modifier = Modifier,
    secondary: @Composable RowScope.() -> Unit,
    primary: @Composable RowScope.() -> Unit,
) {
    val left = leftHanded()
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        if (left) {
            primary()
            Box(Modifier.weight(1f))
            secondary()
        } else {
            secondary()
            Box(Modifier.weight(1f))
            primary()
        }
    }
}

/** Where a floating action button belongs for this user's grip. */
@Composable
fun fabAlignment(): Alignment = if (leftHanded()) Alignment.BottomStart else Alignment.BottomEnd

@Composable
fun quietOutline(): Color = Outline
