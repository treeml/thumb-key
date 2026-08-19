package com.nightshift.tracker.ui.jobs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.components.InkCaptureDialog
import com.nightshift.tracker.ui.components.InkPreview
import androidx.compose.foundation.layout.height
import com.nightshift.tracker.ui.capture.CaptureBar
import com.nightshift.tracker.ui.design.HandedActions
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.NsCard
import com.nightshift.tracker.ui.design.NsChip
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.settings.AppSettings
import com.nightshift.tracker.ui.settings.leftHanded
import com.nightshift.tracker.ui.components.PriorityPicker
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.UrgentRed
import com.nightshift.tracker.ui.theme.priorityColor
import kotlinx.coroutines.delay

private val statusLabels = listOf("Not started", "In progress", "Done")

/**
 * Beds sort naturally: 2 before 10, letters after numbers, unbedded last.
 * A plain string sort puts bed 10 before bed 2, which is exactly the kind of
 * small wrongness that makes a list feel untrustworthy on a round.
 */
private fun bedRank(bed: String): Pair<Int, String> {
    val trimmed = bed.trim()
    if (trimmed.isEmpty()) return Int.MAX_VALUE to ""
    val leadingNumber = trimmed.takeWhile { it.isDigit() }.toIntOrNull()
    return (leadingNumber ?: (Int.MAX_VALUE - 1)) to trimmed.lowercase()
}

private fun jobOrder(jobs: List<Job>, now: Long): List<Job> =
    jobs.sortedWith(
        compareBy(
            { if (it.timerEndAt != null && it.timerEndAt <= now) 0 else 1 },
            { it.priority },
            { it.createdAt },
        ),
    )

@Composable
fun JobsTab(
    vm: MainViewModel,
    generation: Int,
) {
    val jobs = vm.jobs.collectAsStateValue()
    val context = LocalContext.current
    val byBed by AppSettings.groupJobsByBed.collectAsStateWithLifecycle()
    val seed = vm.captureSeed.collectAsStateValue()
    val lockedBed = vm.lockedBed.collectAsStateValue()
    val rounds = vm.rounds.collectAsStateValue()

    // Beds already in play this shift — from the round list and existing jobs —
    // so pinning one after a paper round is a tap, not typing.
    val knownBeds =
        (rounds.map { it.bed } + jobs.map { it.bed })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy({ bedRank(it).first }, { bedRank(it).second }))
    val now = System.currentTimeMillis()

    val open = jobs.filter { it.status != 2 }
    val completed = jobs.filter { it.status == 2 }
    var showCompleted by rememberSaveable { mutableStateOf(false) }

    // Urgency view: anything whose timer has blown floats to the top, whatever
    // its priority. Nothing else re-orders under the user's thumb mid-round.
    val flat = jobOrder(open, now)
    val beds =
        open
            .groupBy { it.bed.trim() }
            .toList()
            .sortedWith(compareBy({ bedRank(it.first).first }, { bedRank(it.first).second }))

    Column(Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("Order", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            SortPill("Urgency", !byBed) { AppSettings.setGroupJobsByBed(context, false) }
            SortPill("By bed", byBed) { AppSettings.setGroupJobsByBed(context, true) }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (jobs.isEmpty()) {
                    item {
                        Text(
                            "Nothing on the list.\n\nType or dictate a line below — " +
                                "\"b12 chase K+ !1 30m\" becomes bed 12, urgent, with a " +
                                "30-minute alarm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }

                if (byBed) {
                    beds.forEach { (bed, bedJobs) ->
                        item(key = "bed-$bed") {
                            BedHeader(
                                bed = bed,
                                jobs = bedJobs,
                                now = now,
                                onAdd = { vm.startJobForBed(bed) },
                            )
                        }
                        items(jobOrder(bedJobs, now), key = { it.id }) { job ->
                            JobCard(job = job, vm = vm, generation = generation)
                        }
                    }
                } else {
                    items(flat, key = { it.id }) { job ->
                        JobCard(job = job, vm = vm, generation = generation)
                    }
                }

                if (completed.isNotEmpty()) {
                    item(key = "completed-drawer") {
                        CompletedDrawerHeader(
                            title = "Completed (${completed.size})",
                            expanded = showCompleted,
                            onToggle = { showCompleted = !showCompleted },
                        )
                    }
                    if (showCompleted) {
                        items(completed, key = { "done-${it.id}" }) { job ->
                            CompletedJobRow(job = job, vm = vm)
                        }
                    }
                }
                item(key = "blank-job") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .background(Surface2, RoundedCornerShape(12.dp))
                            .clickable { vm.addJob() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+ Blank job card",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
        CaptureBar(
            onCapture = { vm.captureJob(it) },
            seed = seed,
            onSeedConsumed = { vm.clearCaptureSeed() },
            lockedBed = lockedBed,
            onBedChange = { vm.setLockedBed(it) },
            knownBeds = knownBeds,
        )
    }
}

@Composable
private fun SortPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Surface2,
                RoundedCornerShape(10.dp),
            ).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
        )
    }
}

/** Heading for one bed's jobs, with a shortcut to add another for that bed. */
@Composable
private fun BedHeader(
    bed: String,
    jobs: List<Job>,
    now: Long,
    onAdd: () -> Unit,
) {
    val overdue = jobs.count { it.timerEndAt != null && it.timerEndAt <= now }
    val urgent = jobs.count { it.priority == 1 }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Text(
            if (bed.isBlank()) "No bed" else "Bed $bed",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "${jobs.size}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        if (overdue > 0) NsChip("$overdue overdue", UrgentRed, strong = true)
        if (urgent > 0 && overdue == 0) NsChip("$urgent urgent", UrgentRed)
        Box(Modifier.weight(1f))
        NsAction("+ Job", onAdd, haptic = false)
    }
}

@Composable
private fun JobCard(
    job: Job,
    vm: MainViewModel,
    generation: Int,
) {
    // A filled-in job collapses to a single readable line; a blank one opens
    // ready to type. Detail is one tap away, never in the way.
    var expanded by rememberSaveable(job.id) { mutableStateOf(job.text.isBlank() && job.inkJson == null) }
    var showInk by remember { mutableStateOf(false) }
    var showTimerPicker by remember { mutableStateOf(false) }
    val accent = priorityColor(job.priority)
    val overdue = job.timerEndAt != null && job.timerEndAt <= System.currentTimeMillis()
    val left = leftHanded()

    NsCard(accent = if (overdue) UrgentRed else null) {
        Column(Modifier.animateContentSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                modifier = Modifier.fillMaxWidth().padding(Space.md),
            ) {
                val summary: @Composable () -> Unit = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                    ) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(accent, RoundedCornerShape(2.dp)),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                job.text.ifBlank { if (job.inkJson != null) "Handwritten note" else "New job" },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = if (expanded) 4 else 2,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                                if (job.bed.isNotBlank()) {
                                    Text(
                                        "Bed ${job.bed}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }
                                if (job.status == 1) {
                                    Text(
                                        "IN PROGRESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                TimerLabel(job)
                            }
                        }
                    }
                }
                val doneButton: @Composable () -> Unit = {
                    NsAction(
                        label = "Done",
                        onClick = { vm.completeJob(job) },
                        tone = RoutineGreen,
                        filled = true,
                    )
                }
                if (left) {
                    doneButton()
                    Box(Modifier.weight(1f)) { summary() }
                } else {
                    Box(Modifier.weight(1f)) { summary() }
                    doneButton()
                }
            }

            if (overdue) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier.padding(start = Space.md, end = Space.md, bottom = Space.md),
                ) {
                    NsAction("+10 min", { vm.snoozeJob(job, 10) }, tone = UrgentRed)
                    NsAction("+30 min", { vm.snoozeJob(job, 30) }, tone = UrgentRed)
                    NsAction("Clear timer", { vm.setJobTimer(job, null) })
                }
            }

            if (expanded) {
                Column(
                    Modifier.padding(start = Space.md, end = Space.md, bottom = Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    DbTextField(
                        value = job.text,
                        onCommit = { vm.updateJob(job.copy(text = it)) },
                        label = "Job",
                        seedKey = "${job.id}-$generation-text",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        DbTextField(
                            value = job.bed,
                            onCommit = { vm.updateJob(job.copy(bed = it)) },
                            label = "Bed",
                            seedKey = "${job.id}-$generation-bed",
                            singleLine = true,
                            modifier = Modifier.width(110.dp),
                        )
                        StatusToggle(status = job.status) {
                            vm.updateJob(job.copy(status = (job.status + 1) % 2))
                        }
                    }
                    job.inkJson?.let { ink ->
                        InkPreview(json = ink, modifier = Modifier.clickable { showInk = true })
                    }
                    PriorityPicker(selected = job.priority, onSelect = { vm.updateJob(job.copy(priority = it)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        TimerButton(job = job, onClick = { showTimerPicker = true })
                        NsAction("Ink", { showInk = true }, icon = Icons.Filled.Draw)
                    }
                    HandedActions(
                        secondary = { ArmedDeleteButton(onConfirmedDelete = { vm.deleteJobWithUndo(job) }) },
                        primary = {
                            NsAction(
                                label = "Collapse",
                                onClick = { expanded = false },
                                haptic = false,
                            )
                        },
                    )
                }
            }
        }
    }

    if (showInk) {
        InkCaptureDialog(
            initialJson = job.inkJson,
            onDismiss = { showInk = false },
            onSave = {
                vm.updateJob(job.copy(inkJson = it))
                showInk = false
            },
        )
    }
    if (showTimerPicker) {
        TimerPickerDialog(
            onDismiss = { showTimerPicker = false },
            onClear = if (job.timerEndAt != null) {
                {
                    vm.setJobTimer(job, null)
                    showTimerPicker = false
                }
            } else {
                null
            },
            onPick = { minutes ->
                vm.setJobTimer(job, System.currentTimeMillis() + minutes * 60_000L)
                showTimerPicker = false
            },
        )
    }
}

/** Live countdown shown inline on the collapsed card. */
@Composable
private fun TimerLabel(job: Job) {
    val end = job.timerEndAt ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remaining = end - now
    if (remaining <= 0) {
        Text("TIME UP", style = MaterialTheme.typography.labelSmall, color = UrgentRed)
    } else {
        Text(
            "%d:%02d".format(remaining / 60_000, (remaining % 60_000) / 1000),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
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
                .defaultMinSize(minHeight = 48.dp)
                .background(Surface2, RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                .background(Surface1, RoundedCornerShape(12.dp))
                .border(1.dp, Outline, RoundedCornerShape(12.dp))
                .padding(12.dp),
    ) {
        Text("✓", style = MaterialTheme.typography.titleMedium, color = RoutineGreen)
        Text(
            (if (job.bed.isNotBlank()) "Bed ${job.bed} — " else "") +
                job.text.ifBlank { "(handwritten note)" },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .defaultMinSize(minHeight = 44.dp)
                .background(Surface2, RoundedCornerShape(10.dp))
                .clickable { vm.reopenJob(job) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Restore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        ArmedDeleteButton(onConfirmedDelete = { vm.deleteJobWithUndo(job) }, idleLabel = "Del")
    }
}

@Composable
private fun StatusToggle(
    status: Int,
    onToggle: () -> Unit,
) {
    val color =
        when (status) {
            2 -> RoutineGreen
            1 -> MaterialTheme.colorScheme.primary
            else -> TextSecondary
        }
    Box(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(statusLabels[status], style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun TimerButton(
    job: Job,
    onClick: () -> Unit,
) {
    // Ticker so the countdown text stays live while a timer runs.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(job.timerEndAt) {
        while (job.timerEndAt != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val end = job.timerEndAt
    val label: String
    val color: androidx.compose.ui.graphics.Color
    if (end == null) {
        label = "Timer"
        color = TextSecondary
    } else {
        val remaining = end - now
        if (remaining <= 0) {
            label = "TIME UP"
            color = UrgentRed
        } else {
            val m = remaining / 60_000
            val s = (remaining % 60_000) / 1000
            label = "%d:%02d".format(m, s)
            color = MaterialTheme.colorScheme.primary
        }
    }
    Box(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Alarm, contentDescription = null, tint = color, modifier = Modifier.width(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun TimerPickerDialog(
    onDismiss: () -> Unit,
    onClear: (() -> Unit)?,
    onPick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Job timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Audible alarm when it expires — e.g. recheck BP in 30 min.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { m -> MinuteChip(m, onPick) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(45, 60, 90, 120).forEach { m -> MinuteChip(m, onPick) }
                }
            }
        },
        confirmButton = {
            if (onClear != null) TextButton(onClick = onClear) { Text("Cancel timer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = Surface2,
    )
}

@Composable
private fun MinuteChip(
    minutes: Int,
    onPick: (Int) -> Unit,
) {
    Box(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(Surface1, RoundedCornerShape(12.dp))
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .clickable { onPick(minutes) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$minutes m", style = MaterialTheme.typography.labelLarge)
    }
}

// Small helper to keep call sites tidy.
@Composable
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateValue(): T = collectAsState().value
