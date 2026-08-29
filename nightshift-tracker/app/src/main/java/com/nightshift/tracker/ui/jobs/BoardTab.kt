package com.nightshift.tracker.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Bed
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.board.BoardItem
import com.nightshift.tracker.ui.board.boardOrder
import com.nightshift.tracker.ui.capture.CaptureBar
import com.nightshift.tracker.ui.capture.bedLabel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.NsChip
import com.nightshift.tracker.ui.design.Radius
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.design.dueColor
import com.nightshift.tracker.ui.design.dueText
import com.nightshift.tracker.ui.design.isOverdue
import com.nightshift.tracker.ui.design.rememberNow
import com.nightshift.tracker.ui.design.rememberTick
import com.nightshift.tracker.ui.reviews.CompletedReviewRow
import com.nightshift.tracker.ui.theme.Accent
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.UrgentRed

/**
 * Beds sort naturally: 2 before 10, letters after numbers.
 * A plain string sort puts bed 10 before bed 2, which is exactly the kind of
 * small wrongness that makes a list feel untrustworthy on a round.
 */
internal fun bedRank(label: String): Pair<Int, String> {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return Int.MAX_VALUE to ""
    val leadingNumber = trimmed.takeWhile { it.isDigit() }.toIntOrNull()
    return (leadingNumber ?: (Int.MAX_VALUE - 1)) to trimmed.lowercase()
}

/**
 * The board: everything outstanding, jobs and reviews together, under the bed
 * it belongs to.
 *
 * They were on separate tabs, which meant the answer to "what have I still got
 * on?" lived in two places and you had to remember to check the other one. A
 * clinical review is a thing you have to do; so is chasing a potassium. The
 * only real difference is what opens when you tap it.
 */
@Composable
fun BoardTab(
    vm: MainViewModel,
    generation: Int,
) {
    val jobs = vm.jobs.collectAsStateValue()
    val reviews = vm.reviews.collectAsStateValue()
    val beds = vm.beds.collectAsStateValue()
    val seed = vm.captureSeed.collectAsStateValue()
    val target = vm.captureTarget.collectAsStateValue()
    // Ticks, so the board re-sorts itself as deadlines come round.
    val now = rememberNow()

    val items =
        jobs.filter { it.status != 2 }.map { BoardItem.JobItem(it) } +
            reviews.filter { !it.done }.map { BoardItem.ReviewItem(it) }
    val doneJobs = jobs.filter { it.status == 2 }
    val doneReviews = reviews.filter { it.done }
    val doneCount = doneJobs.size + doneReviews.size

    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var editingBed by remember { mutableStateOf<Bed?>(null) }

    // Group by the bed text itself rather than by a bed row, so a review that
    // only ever had "56" typed into it still files under bed 56.
    val grouped = items.groupBy { it.bedText.trim().uppercase() }
    val bedGroups =
        grouped.keys
            .filter { it.isNotBlank() }
            .sortedWith(compareBy({ bedRank(it).first }, { bedRank(it).second }))
    val loose = grouped[""].orEmpty()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "new-review") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        NsAction(
                            label = "Review",
                            onClick = { vm.openNewReview() },
                            icon = Icons.Filled.Add,
                            tone = Accent,
                        )
                    }
                }

                if (items.isEmpty()) {
                    item {
                        Text(
                            "Nothing on the list.\n\nType it all into one line below — " +
                                "\"b56 MB 122484 chase potassium 0400\" — and the bed, the " +
                                "patient, the MRN and the alarm come out of it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp),
                        )
                    }
                }

                bedGroups.forEach { key ->
                    val groupItems = grouped[key].orEmpty()
                    val bed = beds.firstOrNull { it.label.trim().equals(key, ignoreCase = true) }
                    item(key = "bed-$key") {
                        BedHeader(
                            label = groupItems.first().bedText,
                            bed = bed,
                            items = groupItems,
                            now = now,
                            onEdit = { bed?.let { editingBed = it } },
                        )
                    }
                    items(boardOrder(groupItems, now), key = { it.key }) { item ->
                        BoardRow(item = item, vm = vm, now = now)
                    }
                }

                if (loose.isNotEmpty()) {
                    item(key = "loose-header") { GroupHeading("NO BED (${loose.size})") }
                    items(boardOrder(loose, now), key = { it.key }) { item ->
                        BoardRow(item = item, vm = vm, now = now)
                    }
                }

                if (doneCount > 0) {
                    item(key = "completed-drawer") {
                        Box(Modifier.padding(top = 12.dp)) {
                            CompletedDrawerHeader(
                                title = "Completed ($doneCount)",
                                expanded = showCompleted,
                                onToggle = { showCompleted = !showCompleted },
                            )
                        }
                    }
                    if (showCompleted) {
                        items(doneJobs, key = { "done-job-${it.id}" }) { job ->
                            CompletedJobRow(job = job, vm = vm)
                        }
                        items(doneReviews, key = { "done-rev-${it.id}" }) { review ->
                            CompletedReviewRow(review = review, vm = vm)
                        }
                    }
                }
            }
        }

        CaptureBar(
            onCapture = { vm.captureJob(it) },
            seed = seed,
            onSeedConsumed = { vm.clearCaptureSeed() },
            targetLabel = target?.label,
        )
    }

    editingBed?.let { bed ->
        BedDialog(
            bed = bed,
            generation = generation,
            onDismiss = { editingBed = null },
            onSave = { vm.updateBed(it) },
            onDelete = {
                vm.deleteBedWithUndo(bed)
                editingBed = null
            },
        )
    }
}

@Composable
private fun GroupHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp),
    )
}

/**
 * The bed line: location, who is in it, and how much is outstanding — all of
 * which the capture bar filled in on its own. Tap it to correct any of that.
 */
@Composable
private fun BedHeader(
    label: String,
    bed: Bed?,
    items: List<BoardItem>,
    now: Long,
    onEdit: () -> Unit,
) {
    val overdue = items.count { isOverdue(it.dueAt, now) }
    val urgent = items.count { it.priority == 1 }
    val who =
        listOfNotNull(bed?.patientName, bed?.mrn).filter { it.isNotBlank() }.joinToString(" · ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clickable(enabled = bed != null, onClick = onEdit)
                .padding(start = 6.dp, end = 6.dp, bottom = 2.dp),
    ) {
        Text(
            bedLabel(label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (who.isNotBlank()) {
            Text(
                who,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Box(Modifier.weight(1f))
        when {
            overdue > 0 -> NsChip("$overdue overdue", UrgentRed, strong = true)
            urgent > 0 -> NsChip("$urgent urgent", UrgentRed)
        }
    }
}

/**
 * One outstanding thing, one line — job or review.
 *
 * Swipe left to finish, swipe right to flag urgent (and again to put it back).
 * Tap opens it full screen. Nothing is editable here, which is the point: this
 * list is for reading and scrolling past, not for typing into.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardRow(
    item: BoardItem,
    vm: MainViewModel,
    now: Long,
) {
    val tick = rememberTick()
    val due = item.dueAt
    val overdue = isOverdue(due, now)
    val urgency = dueColor(due, item.priority, now)

    val dismiss =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        tick()
                        when (item) {
                            is BoardItem.JobItem -> vm.completeJob(item.job)
                            is BoardItem.ReviewItem -> vm.completeReview(item.review)
                        }
                        true
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        tick()
                        val flipped = if (item.priority == 1) 2 else 1
                        when (item) {
                            is BoardItem.JobItem -> vm.updateJob(item.job.copy(priority = flipped))
                            is BoardItem.ReviewItem -> vm.updateReview(item.review.copy(priority = flipped))
                        }
                        false
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            },
        )

    SwipeToDismissBox(
        state = dismiss,
        backgroundContent = { SwipeBackdrop(dismiss.dismissDirection, item.priority) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (due != null) urgency.copy(alpha = 0.10f) else Surface1,
                        RoundedCornerShape(Radius.sm),
                    ).clickable {
                        when (item) {
                            is BoardItem.JobItem -> vm.openJob(item.job)
                            is BoardItem.ReviewItem -> vm.openReview(item.review)
                        }
                    }.padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(urgency, RoundedCornerShape(2.dp)),
            )
            if (item is BoardItem.ReviewItem) {
                // The one thing that has to be obvious at a glance: this is a
                // patient to go and see, not a task to tick off.
                NsChip("REVIEW", Accent)
            }
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (item.inProgress) {
                Text(
                    "···",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (due != null) {
                Text(
                    dueText(due, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = urgency,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Open",
                tint = if (overdue) urgency else Outline,
                modifier = Modifier.width(18.dp),
            )
        }
    }
}

/** What sits behind a row mid-swipe, so the gesture says what it will do. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackdrop(
    direction: SwipeToDismissBoxValue,
    priority: Int,
) {
    val (tone, label, align) =
        when (direction) {
            SwipeToDismissBoxValue.EndToStart ->
                Triple(RoutineGreen, "DONE ✓", Alignment.CenterEnd as Alignment)
            SwipeToDismissBoxValue.StartToEnd ->
                Triple(
                    UrgentRed,
                    if (priority == 1) "NORMAL" else "URGENT",
                    Alignment.CenterStart as Alignment,
                )
            SwipeToDismissBoxValue.Settled -> return
        }
    Box(
        Modifier
            .fillMaxSize()
            .background(tone.copy(alpha = 0.18f), RoundedCornerShape(Radius.sm))
            .padding(horizontal = Space.lg),
        contentAlignment = align,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = tone)
    }
}

/** Correcting what the capture bar guessed, without a permanent form on screen. */
@Composable
private fun BedDialog(
    bed: Bed,
    generation: Int,
    onDismiss: () -> Unit,
    onSave: (Bed) -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(bedLabel(bed.label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                DbTextField(
                    value = bed.label,
                    onCommit = { onSave(bed.copy(label = it)) },
                    label = "Bed",
                    seedKey = "${bed.id}-$generation-label",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DbTextField(
                    value = bed.patientName,
                    onCommit = { onSave(bed.copy(patientName = it)) },
                    label = "Patient / initials",
                    seedKey = "${bed.id}-$generation-name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DbTextField(
                    value = bed.mrn,
                    onCommit = { onSave(bed.copy(mrn = it)) },
                    label = "MRN",
                    seedKey = "${bed.id}-$generation-mrn",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Removing the bed keeps its jobs — they move to NO BED.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                ArmedDeleteButton(onConfirmedDelete = onDelete, idleLabel = "Remove bed")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = Surface2,
    )
}

@Composable
fun CompletedDrawerHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .background(Surface2, RoundedCornerShape(Radius.sm))
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (expanded) "Hide" else "Show",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CompletedJobRow(
    job: Job,
    vm: MainViewModel,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(Radius.sm))
                .border(1.dp, Outline, RoundedCornerShape(Radius.sm))
                .padding(10.dp),
    ) {
        Text("✓", style = MaterialTheme.typography.bodyMedium, color = RoutineGreen)
        Text(
            (if (job.bed.isNotBlank()) bedLabel(job.bed) + " — " else "") +
                job.text.ifBlank { "(no text)" },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .defaultMinSize(minHeight = 40.dp)
                .background(Surface2, RoundedCornerShape(Radius.sm))
                .clickable { vm.reopenJob(job) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Restore", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        ArmedDeleteButton(onConfirmedDelete = { vm.deleteJobWithUndo(job) }, idleLabel = "Del")
    }
}

// Small helper to keep call sites tidy.
@Composable
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateValue(): T = collectAsState().value
