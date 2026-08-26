package com.nightshift.tracker.ui.reviews

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.components.DueTimeDialog
import com.nightshift.tracker.ui.components.PriorityPicker
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.nightshift.tracker.ai.AiFactory
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.NsChip
import com.nightshift.tracker.ui.design.dueColor
import com.nightshift.tracker.ui.design.dueText
import com.nightshift.tracker.ui.design.isOverdue
import kotlinx.coroutines.delay
import com.nightshift.tracker.ui.design.SectionLabel
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.design.fabAlignment
import com.nightshift.tracker.ui.jobs.CompletedDrawerHeader
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.theme.CardBody
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.priorityColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun buildDhrNote(review: Review): String {
    val now = Date()
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    val abcde =
        listOf("A" to review.a, "B" to review.b, "C" to review.c, "D" to review.d, "E" to review.e)
            .filter { it.second.isNotBlank() }
            .joinToString("\n") { "${it.first}: ${it.second.trim()}" }
    return buildString {
        appendLine("OVERNIGHT CLINICAL REVIEW")
        appendLine("Date: $date  Time: $time")
        appendLine()
        appendLine("Patient: ${review.patientName.trim()} | Bed ${review.bed.trim()} | MRN: ${review.mrn.trim()}")
        appendLine()
        appendLine("Reason for review:")
        appendLine(review.reason.trim())
        appendLine()
        if (abcde.isNotBlank()) {
            appendLine("Assessment (ABCDE):")
            appendLine(abcde)
            appendLine()
        }
        if (review.investigations.isNotBlank()) {
            appendLine("Investigations:")
            appendLine(review.investigations.trim())
            appendLine()
        }
        appendLine("Impression:")
        appendLine(review.impression.trim())
        appendLine()
        appendLine("Plan:")
        appendLine(review.plan.trim())
        if (review.escalatedAt != null) {
            appendLine()
            appendLine(
                "Escalated to ${review.escalatedTo.ifBlank { "registrar" }} at " +
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(review.escalatedAt)) + ".",
            )
        } else if (review.registrarNotified) {
            appendLine()
            appendLine("Registrar notified and aware.")
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
fun ReviewsTab(
    vm: MainViewModel,
    generation: Int,
) {
    val reviews = vm.reviews.collectAsStateValue()
    val active = reviews.filter { !it.done }
    val completed = reviews.filter { it.done }
    var showCompleted by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "templates") { TemplateRow(vm) }
            if (reviews.isEmpty()) {
                item {
                    Text(
                        "No reviews this shift. Start from a template above, or a blank card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(active, key = { it.id }) { review ->
                ReviewCard(review = review, vm = vm, generation = generation)
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
                    items(completed, key = { "done-${it.id}" }) { review ->
                        CompletedReviewRow(review = review, vm = vm)
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { vm.addReview() },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Blank review") },
            modifier = Modifier.align(fabAlignment()).padding(20.dp),
        )
    }
}

/**
 * The calls that come in over and over, one tap from a started review. A
 * template fills the reason, a starting priority and a workup PROMPT — never
 * a finding.
 */
@Composable
private fun TemplateRow(vm: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        SectionLabel("START FROM")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            reviewTemplates.forEach { template ->
                NsAction(
                    label = template.label,
                    onClick = { vm.addReviewFromTemplate(template) },
                    tone = priorityColor(template.priority),
                    filled = true,
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    vm: MainViewModel,
    generation: Int,
) {
    // Filled reviews collapse to their header; a fresh one opens ready to type.
    var expanded by rememberSaveable(review.id) {
        mutableStateOf(review.reason.isBlank() && review.impression.isBlank())
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

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(16.dp))
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .animateContentSize(),
    ) {
        // Collapsed header — always visible, big touch target.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
        ) {
            Box(Modifier.size(14.dp).background(accent, CircleShape))
            Column(Modifier.weight(1f)) {
                val bedLabel = if (review.bed.isBlank()) "Bed —" else "Bed ${review.bed}"
                Text(
                    "$bedLabel  ·  ${review.patientName.ifBlank { "Unnamed" }}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (review.reason.isNotBlank()) {
                    Text(
                        review.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = if (expanded) 10 else 1,
                    )
                }
                if (remind != null) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        NsChip(dueText(remind, now), accent, strong = isOverdue(remind, now))
                    }
                }
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSecondary,
            )
        }

        if (expanded) {
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                    ArmedDeleteButton(onConfirmedDelete = { vm.deleteReviewWithUndo(review) })
                    // Done: card moves to the Completed drawer, retrievable there.
                    Box(
                        Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .background(RoutineGreen.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                            .border(1.dp, RoutineGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { vm.completeReview(review) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Done ✓", style = MaterialTheme.typography.labelLarge, color = RoutineGreen)
                    }
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
            }
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

@Composable
private fun CompletedReviewRow(
    review: Review,
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
        Box(Modifier.size(12.dp).background(priorityColor(review.priority), CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                "Bed ${review.bed.ifBlank { "—" }} · ${review.patientName.ifBlank { "Unnamed" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
            )
            if (review.reason.isNotBlank()) {
                Text(
                    review.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }
        Box(
            Modifier
                .defaultMinSize(minHeight = 44.dp)
                .background(Surface2, RoundedCornerShape(10.dp))
                .clickable { vm.reopenReview(review) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Restore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        ArmedDeleteButton(onConfirmedDelete = { vm.deleteReviewWithUndo(review) }, idleLabel = "Del")
    }
}

@Composable
private fun AbcdeField(
    cheat: AbcdeCheat,
    review: Review,
    vm: MainViewModel,
    generation: Int,
) {
    var showCheat by rememberSaveable("${review.id}-${cheat.letter}") { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val value =
                when (cheat.letter) {
                    "A" -> review.a
                    "B" -> review.b
                    "C" -> review.c
                    "D" -> review.d
                    else -> review.e
                }
            DbTextField(
                value = value,
                onCommit = { text ->
                    vm.updateReview(
                        when (cheat.letter) {
                            "A" -> review.copy(a = text)
                            "B" -> review.copy(b = text)
                            "C" -> review.copy(c = text)
                            "D" -> review.copy(d = text)
                            else -> review.copy(e = text)
                        },
                    )
                },
                label = "${cheat.letter} — ${cheat.title}",
                seedKey = "${review.id}-$generation-${cheat.letter}",
                modifier = Modifier.weight(1f),
            )
            // The "?" toggle: expands the 4 am cheat sheet.
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        if (showCheat) cheat.color.copy(alpha = 0.25f) else Surface2,
                        CircleShape,
                    ).border(1.dp, if (showCheat) cheat.color else Outline, CircleShape)
                    .clickable { showCheat = !showCheat },
                contentAlignment = Alignment.Center,
            ) {
                Text("?", style = MaterialTheme.typography.titleMedium, color = if (showCheat) cheat.color else TextSecondary)
            }
        }
        if (showCheat) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(cheat.color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, cheat.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                cheat.lines.forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = cheat.color, style = MaterialTheme.typography.bodyMedium)
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = CardBody)
                    }
                }
            }
        }
    }
}
