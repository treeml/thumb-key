package com.nightshift.tracker.ui.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ai.AiFactory
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.capture.bedLabel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.components.DueTimeDialog
import com.nightshift.tracker.ui.components.PriorityPicker
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.NsChip
import com.nightshift.tracker.ui.design.dueColor
import com.nightshift.tracker.ui.design.dueText
import com.nightshift.tracker.ui.design.isOverdue
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One review, full screen.
 *
 * A review is a form — bed, name, MRN, ABCDE, impression, plan, escalation —
 * and a form has no business living inside a scrolling list of other people's
 * jobs. On its own screen it can be the only thing on screen, which is what
 * you want at 4 am with a deteriorating patient and one hand free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(
    vm: MainViewModel,
    reviewId: String,
    generation: Int,
) {
    val reviews = vm.reviews.collectAsStateValue()
    val review = reviews.firstOrNull { it.id == reviewId }
    if (review == null) {
        LaunchedEffect(Unit) { vm.screen.value = Screen.ActiveShift }
        return
    }

    var showReminder by rememberSaveable(review.id) { mutableStateOf(false) }
    val context = LocalContext.current

    // A review with an alarm on it warms from green to red like a job does —
    // "recheck the potassium at 0400" is a deadline, not a preference.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(review.remindAt) {
        while (review.remindAt != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remind = review.remindAt
    val accent = dueColor(remind, review.priority, now)
    val back = { vm.screen.value = Screen.ActiveShift }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to the board")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(accent, CircleShape))
                        Text(
                            "  " + (if (review.bed.isBlank()) "Review" else bedLabel(review.bed)) +
                                review.patientName.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (remind != null) {
                NsChip(dueText(remind, now), accent, strong = isOverdue(remind, now))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DbTextField(
                    value = review.bed,
                    onCommit = { vm.updateReview(review.copy(bed = it)) },
                    label = "Bed",
                    seedKey = "${review.id}-$generation-bed",
                    singleLine = true,
                    modifier = Modifier.width(90.dp),
                )
                DbTextField(
                    value = review.patientName,
                    onCommit = { vm.updateReview(review.copy(patientName = it)) },
                    label = "Name",
                    seedKey = "${review.id}-$generation-name",
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                DbTextField(
                    value = review.mrn,
                    onCommit = { vm.updateReview(review.copy(mrn = it)) },
                    label = "MRN",
                    seedKey = "${review.id}-$generation-mrn",
                    singleLine = true,
                    modifier = Modifier.width(110.dp),
                )
            }
            DbTextField(
                value = review.reason,
                onCommit = { vm.updateReview(review.copy(reason = it)) },
                label = "Reason for review",
                seedKey = "${review.id}-$generation-reason",
                modifier = Modifier.fillMaxWidth(),
            )
            templateFor(review.templateKey)?.let { GuidancePanel(it) }

            PriorityPicker(selected = review.priority, onSelect = { vm.updateReview(review.copy(priority = it)) })

            // Alarm on the patient, not on a task: "come back to bed 12 at
            // 0400". Same absolute-deadline mechanism as a job timer, so it
            // survives a force-quit and a reboot.
            NsAction(
                label = if (remind == null) "Set a reminder" else "Reminder ${dueText(remind, now)}",
                onClick = { showReminder = true },
                icon = Icons.Filled.Alarm,
                tone = if (remind == null) null else accent,
                filled = remind != null,
            )

            Text("ABCDE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            abcdeCheats.forEach { cheat ->
                AbcdeField(cheat = cheat, review = review, vm = vm, generation = generation)
            }

            DbTextField(
                value = review.investigations,
                onCommit = { vm.updateReview(review.copy(investigations = it)) },
                label = "Investigations & results",
                seedKey = "${review.id}-$generation-ix",
                modifier = Modifier.fillMaxWidth(),
            )
            DbTextField(
                value = review.impression,
                onCommit = { vm.updateReview(review.copy(impression = it)) },
                label = "Impression",
                seedKey = "${review.id}-$generation-imp",
                modifier = Modifier.fillMaxWidth(),
            )
            DbTextField(
                value = review.plan,
                onCommit = { vm.updateReview(review.copy(plan = it)) },
                label = "Plan",
                seedKey = "${review.id}-$generation-plan",
                modifier = Modifier.fillMaxWidth(),
            )

            // Escalation tracking — time-stamped, because "did you tell
            // anyone, and when" is the question that gets asked afterwards.
            val escalated = review.registrarNotified || review.escalatedAt != null
            val regColor = if (escalated) RoutineGreen else TextSecondary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .background(regColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .border(1.dp, regColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            if (escalated) {
                                vm.clearEscalation(review)
                            } else {
                                vm.recordEscalation(review, review.escalatedTo.ifBlank { "registrar" })
                            }
                        }.padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when {
                            review.escalatedAt != null ->
                                "Escalated ✓ " +
                                    SimpleDateFormat("HH:mm", Locale.getDefault())
                                        .format(Date(review.escalatedAt))
                            review.registrarNotified -> "Registrar notified ✓"
                            else -> "Escalated to?"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = regColor,
                    )
                }
                Spacer(Modifier.weight(1f))
                ArmedDeleteButton(
                    onConfirmedDelete = {
                        vm.deleteReviewWithUndo(review)
                        back()
                    },
                )
            }

            if (escalated) {
                DbTextField(
                    value = review.escalatedTo,
                    onCommit = { vm.updateReview(review.copy(escalatedTo = it)) },
                    label = "Escalated to (name / role)",
                    seedKey = "${review.id}-$generation-esc",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // DHR note generator — appears once there's something to write.
            if (review.impression.isNotBlank() || review.plan.isNotBlank()) {
                NsAction(
                    label = if (AiFactory.AVAILABLE) "Open note — tidy, copy or email" else "Open note — copy or email",
                    onClick = {
                        vm.openNoteReview(buildDhrNote(review), "Clinical review note")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (review.impression.isNotBlank() || review.plan.isNotBlank()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            copyToClipboard(context, "DHR note", buildDhrNote(review))
                            vm.noteCopied()
                        }.padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Generate DHR note → clipboard",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            // No Done button, on purpose: it sat where a thumb lands to close
            // the screen, so finishing a review you were only reading took one
            // mis-tap. Closing is now the safe thing under your thumb.
            Text(
                "Swipe this left on the board to finish it.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            NsAction(
                label = "Close",
                onClick = back,
                haptic = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showReminder) {
        DueTimeDialog(
            title = "Come back to this patient",
            current = review.remindAt,
            onDismiss = { showReminder = false },
            onClear =
                if (review.remindAt != null) {
                    {
                        vm.setReviewReminder(review, null)
                        showReminder = false
                    }
                } else {
                    null
                },
            onPick = { at ->
                vm.setReviewReminder(review, at)
                showReminder = false
            },
        )
    }
}
