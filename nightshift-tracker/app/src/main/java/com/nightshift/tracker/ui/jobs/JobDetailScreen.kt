package com.nightshift.tracker.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.capture.bedLabel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.components.DueTimeDialog
import com.nightshift.tracker.ui.components.PriorityPicker
import com.nightshift.tracker.ui.design.HandedActions
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.SectionLabel
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.design.dueColor
import com.nightshift.tracker.ui.design.dueText
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * One job, on its own screen.
 *
 * Editing used to happen by expanding the card inside the list. That put a form
 * under the keyboard, made the list taller than the screen, and turned every
 * edit into a scroll through rows you could mis-tap. Here nothing scrolls,
 * nothing else is on screen to hit by accident, and the fields sit above the
 * keyboard because the column is laid out from the top with imePadding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    vm: MainViewModel,
    jobId: String,
    generation: Int,
) {
    val jobs = vm.jobs.collectAsStateValue()
    val job = jobs.firstOrNull { it.id == jobId }
    var showDuePicker by remember { mutableStateOf(false) }

    // Deleted from under us (an undo toast, or a swipe on the list): just leave.
    if (job == null) {
        LaunchedEffect(Unit) { vm.screen.value = Screen.ActiveShift }
        return
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(job.timerEndAt) {
        while (job.timerEndAt != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val due = job.timerEndAt
    val urgency = dueColor(due, job.priority, now)
    val back = { vm.screen.value = Screen.ActiveShift }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to the list")
                    }
                },
                title = {
                    Text(
                        if (job.bed.isBlank()) "Job" else bedLabel(job.bed),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            DbTextField(
                value = job.text,
                onCommit = { vm.updateJob(job.copy(text = it)) },
                label = "Job",
                seedKey = "$jobId-$generation-text",
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                DbTextField(
                    value = job.bed,
                    onCommit = { vm.updateJob(job.copy(bed = it)) },
                    label = "Bed",
                    seedKey = "$jobId-$generation-bed",
                    singleLine = true,
                    modifier = Modifier.width(120.dp),
                )
                NsAction(
                    label = if (job.status == 1) "In progress" else "Not started",
                    onClick = { vm.updateJob(job.copy(status = (job.status + 1) % 2)) },
                    tone = if (job.status == 1) MaterialTheme.colorScheme.primary else null,
                    filled = job.status == 1,
                )
            }

            SectionLabel("PRIORITY")
            PriorityPicker(selected = job.priority, onSelect = { vm.updateJob(job.copy(priority = it)) })

            SectionLabel("WHEN")
            NsAction(
                label = if (due == null) "No time set" else dueText(due, now),
                onClick = { showDuePicker = true },
                icon = Icons.Filled.Alarm,
                tone = if (due == null) null else urgency,
                filled = due != null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            // No Done button here, on purpose. It used to sit exactly where a
            // thumb lands to close the screen, so finishing a job you were only
            // reading was a single mis-tap. Swiping is a deliberate gesture and
            // cannot be done by accident; closing is now the safe thing under
            // your thumb, and finishing costs a swipe on the board.
            Text(
                "Swipe this left on the board to finish it.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = Space.sm),
            )

            HandedActions(
                modifier = Modifier.padding(bottom = Space.lg),
                secondary = {
                    ArmedDeleteButton(
                        onConfirmedDelete = {
                            vm.deleteJobWithUndo(job)
                            back()
                        },
                    )
                },
                primary = {
                    NsAction(
                        label = "Close",
                        onClick = back,
                        haptic = false,
                    )
                },
            )
        }
    }

    if (showDuePicker) {
        DueTimeDialog(
            title = "When is this due?",
            current = job.timerEndAt,
            onDismiss = { showDuePicker = false },
            onClear =
                if (job.timerEndAt != null) {
                    {
                        vm.setJobTimer(job, null)
                        showDuePicker = false
                    }
                } else {
                    null
                },
            onPick = { at ->
                vm.setJobTimer(job, at)
                showDuePicker = false
            },
        )
    }
}

