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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.components.InkCaptureDialog
import com.nightshift.tracker.ui.components.InkPreview
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

@Composable
fun JobsTab(
    vm: MainViewModel,
    generation: Int,
) {
    val jobs = vm.jobs.collectAsStateValue()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (jobs.isEmpty()) {
                item {
                    Text(
                        "No jobs yet. Tap New job when the pager goes off.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(jobs, key = { it.id }) { job ->
                JobCard(job = job, vm = vm, generation = generation)
            }
        }
        ExtendedFloatingActionButton(
            onClick = { vm.addJob() },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New job") },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
        )
    }
}

@Composable
private fun JobCard(
    job: Job,
    vm: MainViewModel,
    generation: Int,
) {
    var showInk by remember { mutableStateOf(false) }
    var showTimerPicker by remember { mutableStateOf(false) }
    val accent = priorityColor(job.priority)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(16.dp))
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .padding(14.dp)
                .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Priority edge marker
            Box(
                Modifier
                    .width(5.dp)
                    .defaultMinSize(minHeight = 40.dp)
                    .background(accent, RoundedCornerShape(3.dp)),
            )
            DbTextField(
                value = job.bed,
                onCommit = { vm.updateJob(job.copy(bed = it)) },
                label = "Bed",
                seedKey = "${job.id}-$generation-bed",
                singleLine = true,
                modifier = Modifier.width(96.dp),
            )
            StatusToggle(status = job.status) {
                vm.updateJob(job.copy(status = (job.status + 1) % 3))
            }
        }
        DbTextField(
            value = job.text,
            onCommit = { vm.updateJob(job.copy(text = it)) },
            label = "Job",
            seedKey = "${job.id}-$generation-text",
            modifier = Modifier.fillMaxWidth(),
        )
        job.inkJson?.let { ink ->
            InkPreview(json = ink, modifier = Modifier.clickable { showInk = true })
        }
        PriorityPicker(selected = job.priority, onSelect = { vm.updateJob(job.copy(priority = it)) })
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TimerButton(job = job, onClick = { showTimerPicker = true })
            Box(
                Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .background(Surface2, RoundedCornerShape(12.dp))
                    .clickable { showInk = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Draw, contentDescription = null, tint = TextSecondary, modifier = Modifier.width(18.dp))
                    Text("Ink", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                }
            }
            Spacer(Modifier.weight(1f))
            ArmedDeleteButton(onConfirmedDelete = { vm.deleteJobWithUndo(job) })
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
