package com.nightshift.tracker.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Shift
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.board.BoardItem
import com.nightshift.tracker.ui.board.boardOrder
import com.nightshift.tracker.ui.capture.bedLabel
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.NsChip
import com.nightshift.tracker.ui.design.Radius
import com.nightshift.tracker.ui.design.SectionLabel
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.design.dueColor
import com.nightshift.tracker.ui.design.dueText
import com.nightshift.tracker.ui.design.isOverdue
import com.nightshift.tracker.ui.design.rememberNow
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.theme.Accent
import com.nightshift.tracker.ui.theme.SoonYellow
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.UrgentRed
import java.util.concurrent.TimeUnit

/**
 * Where the shift stands, in one screen.
 *
 * The board tells you everything; this tells you the next thing. It exists
 * because at 3 am the question is never "show me all forty items" — it is
 * "what is about to blow up, how long have I been here, and have I eaten".
 * Everything on it is a way into somewhere else.
 */
@Composable
fun CommandCentre(
    vm: MainViewModel,
    onOpenBoard: () -> Unit,
) {
    val shift = vm.activeShift.collectAsStateValue() ?: return
    val jobs = vm.jobs.collectAsStateValue()
    val reviews = vm.reviews.collectAsStateValue()
    val now = rememberNow(10_000L)

    val items =
        jobs.filter { it.status != 2 }.map { BoardItem.JobItem(it) } +
            reviews.filter { !it.done }.map { BoardItem.ReviewItem(it) }
    val overdue = items.count { isOverdue(it.dueAt, now) }
    val urgent = items.count { it.priority == 1 }
    val openReviews = reviews.count { !it.done }
    val next = boardOrder(items.filter { it.dueAt != null || it.priority == 1 }, now).take(4)
    // The thing that gets people in trouble is a review with no plan written.
    val unplanned = reviews.count { !it.done && it.impression.isBlank() && it.plan.isBlank() }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Space.md),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { ClockCard(shift = shift, now = now, onBreak = { vm.recordBreak() }) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
                Stat("${items.size}", "on the list", if (items.isEmpty()) TextSecondary else Accent, onOpenBoard, Modifier.weight(1f))
                Stat("$overdue", "overdue", if (overdue > 0) UrgentRed else TextSecondary, onOpenBoard, Modifier.weight(1f))
                Stat("$urgent", "urgent", if (urgent > 0) UrgentRed else TextSecondary, onOpenBoard, Modifier.weight(1f))
                Stat("$openReviews", "reviews", if (openReviews > 0) SoonYellow else TextSecondary, onOpenBoard, Modifier.weight(1f))
            }
        }

        if (next.isNotEmpty()) {
            item { SectionLabel("NEXT UP") }
            items(next.size) { i ->
                val item = next[i]
                NextRow(
                    item = item,
                    now = now,
                    onClick = {
                        when (item) {
                            is BoardItem.JobItem -> vm.openJob(item.job)
                            is BoardItem.ReviewItem -> vm.openReview(item.review)
                        }
                    },
                )
            }
        }

        if (unplanned > 0) {
            item {
                Text(
                    "$unplanned review${if (unplanned == 1) "" else "s"} with no impression or " +
                        "plan written yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoonYellow,
                    modifier = Modifier.padding(top = Space.sm),
                )
            }
        }

        item { SectionLabel("GO TO") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    NsAction(
                        label = "New review",
                        onClick = { vm.openNewReview() },
                        icon = Icons.Filled.Add,
                        tone = Accent,
                        filled = true,
                        modifier = Modifier.weight(1f),
                    )
                    NsAction(
                        label = "Handover",
                        onClick = { vm.openHandover() },
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    NsAction(
                        label = "Logbook",
                        onClick = { vm.screen.value = Screen.Logbook },
                        icon = Icons.Filled.MenuBook,
                        modifier = Modifier.weight(1f),
                    )
                    NsAction(
                        label = "End shift",
                        onClick = { vm.openEndShift() },
                        icon = Icons.Filled.DoneAll,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Hours on, and whether anyone has had a break. Tappable, because logging one should cost nothing. */
@Composable
private fun ClockCard(
    shift: Shift,
    now: Long,
    onBreak: () -> Unit,
) {
    val elapsed = now - shift.startedAt
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val mins = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60
    val sinceBreak = shift.lastBreakAt?.let { TimeUnit.MILLISECONDS.toHours(now - it) }
    val breakOverdue = (sinceBreak ?: hours) >= 6

    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(Radius.lg))
            .clickable(onClick = onBreak)
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            "${hours}h ${mins}m on shift",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            when {
                sinceBreak == null && breakOverdue -> "No break yet — tap to log one."
                sinceBreak == null -> "No break logged. Tap when you take one."
                breakOverdue -> "${sinceBreak}h since your last break. Tap to log another."
                else -> "Break ${sinceBreak}h ago. Tap to log another."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (breakOverdue) SoonYellow else TextSecondary,
        )
    }
}

@Composable
private fun Stat(
    value: String,
    label: String,
    tone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(Surface1, RoundedCornerShape(Radius.md))
            .border(1.dp, tone.copy(alpha = 0.35f), RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .padding(vertical = Space.md, horizontal = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = tone)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NextRow(
    item: BoardItem,
    now: Long,
    onClick: () -> Unit,
) {
    val due = item.dueAt
    val urgency = dueColor(due, item.priority, now)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (due != null) urgency.copy(alpha = 0.10f) else Surface1,
                    RoundedCornerShape(Radius.sm),
                ).border(1.dp, urgency.copy(alpha = 0.30f), RoundedCornerShape(Radius.sm))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (item is BoardItem.ReviewItem) NsChip("REVIEW", Accent)
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.bedText.isNotBlank()) {
                Text(
                    bedLabel(item.bedText),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
        Text(
            if (due != null) dueText(due, now) else "URGENT",
            style = MaterialTheme.typography.labelSmall,
            color = urgency,
        )
    }
}

