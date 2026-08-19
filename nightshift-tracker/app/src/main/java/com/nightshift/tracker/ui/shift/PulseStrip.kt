package com.nightshift.tracker.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.data.Shift
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.SoonYellow
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.UrgentRed
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * The "what's happening right now" line. Always visible, never interactive
 * enough to slow anyone down — the only tappable chip is the break one.
 *
 * It answers the three questions an intern asks themselves every ten minutes:
 * has anything blown up, how much is left, and how long have I been here.
 */
@Composable
fun PulseStrip(
    shift: Shift,
    jobs: List<Job>,
    reviews: List<Review>,
    onBreak: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shift.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    val open = jobs.filter { it.status != 2 }
    val overdue = open.count { it.timerEndAt != null && it.timerEndAt <= now }
    val urgent = open.count { it.priority == 1 }
    val openReviews = reviews.count { !it.done }
    val elapsed = now - shift.startedAt
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val mins = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60
    val sinceBreak = shift.lastBreakAt?.let { TimeUnit.MILLISECONDS.toHours(now - it) }
    val breakOverdue = (sinceBreak ?: hours) >= 6

    Row(
        Modifier
            .fillMaxWidth()
            .background(Ink)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (overdue > 0) Chip("$overdue overdue", UrgentRed, strong = true)
        if (urgent > 0) Chip("$urgent urgent", UrgentRed)
        Chip("${open.size} open", if (open.isEmpty()) TextSecondary else SoonYellow)
        if (openReviews > 0) Chip("$openReviews review${if (openReviews == 1) "" else "s"}", TextSecondary)
        Chip("${hours}h ${mins}m on", TextSecondary)
        Chip(
            text =
                when {
                    sinceBreak == null && breakOverdue -> "no break yet — take one"
                    sinceBreak == null -> "log a break"
                    breakOverdue -> "${sinceBreak}h since break"
                    else -> "break ${sinceBreak}h ago"
                },
            color = if (breakOverdue) SoonYellow else TextSecondary,
            onClick = onBreak,
        )
    }
}

@Composable
private fun Chip(
    text: String,
    color: Color,
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .background(
                color.copy(alpha = if (strong) 0.22f else 0.10f),
                RoundedCornerShape(8.dp),
            ).border(1.dp, if (strong) color else Outline, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
