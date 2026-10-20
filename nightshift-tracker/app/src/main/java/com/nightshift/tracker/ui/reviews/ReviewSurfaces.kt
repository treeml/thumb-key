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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.components.DueTimeDialog
import com.nightshift.tracker.ui.components.PriorityPicker
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
import com.nightshift.tracker.ui.design.rememberTick
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


/**
 * Type the problem, pick the closest match.
 *
 * This replaced a sideways-scrolling strip of chips. Scrolling to find a card
 * is fine with six templates and useless with thirty, and the list only grows;
 * typing four letters does not care how long it is. Matching forgives
 * abbreviations, synonyms and typos (see TemplateSearch), and whatever you
 * typed is always offered as a review of its own — so an unusual call costs
 * one tap rather than sending you off to find the blank-card button.
 */
@Composable
internal fun TemplateSearch(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    val matches = remember(query) { searchTemplates(query) }
    val typed = query.trim()
    // An exact label hit is already the top row; don't offer it twice.
    val offerTyped =
        typed.isNotBlank() && matches.none { it.label.equals(typed, ignoreCase = true) }
    val visible = if (query.isBlank() && !showAll) emptyList() else matches.take(if (showAll) matches.size else 6)

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            SectionLabel("WHAT'S THE PROBLEM?", modifier = Modifier.weight(1f))
            Text(
                if (showAll) "Hide list" else "Show all (${reviewTemplates.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .clickable { showAll = !showAll }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("chest pain, sob, temp, fell…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.clickable { query = "" },
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
                KeyboardActions(
                    onSearch = {
                        // Enter takes the best match, or the words themselves.
                        val best = matches.firstOrNull()
                        if (best != null) {
                            vm.addReviewFromTemplate(best)
                        } else if (typed.isNotBlank()) {
                            vm.addReviewWithReason(typed)
                        }
                        query = ""
                        showAll = false
                        keyboard?.hide()
                    },
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        visible.forEach { template ->
            TemplateRow(
                label = template.label,
                detail = template.reason.takeIf { !it.equals(template.label, ignoreCase = true) },
                // A line of the differential, so you can see the card carries
                // real prompts before you commit to it.
                hint = template.thinkAbout.takeIf { it.isNotBlank() },
                tone = priorityColor(template.priority),
                onClick = {
                    vm.addReviewFromTemplate(template)
                    query = ""
                    showAll = false
                    keyboard?.hide()
                },
            )
        }

        if (typed.isNotBlank() && matches.isEmpty()) {
            Text(
                "Nothing matches — start it as its own review below.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        if (offerTyped) {
            // Deliberately the plainest row on screen. It used to look exactly
            // as substantial as a real template while carrying none of the
            // prompts, which made it easy to pick by mistake and then wonder
            // where the help went.
            TemplateRow(
                label = "Not on the list — start \"$typed\"",
                detail = "Blank review, no prompts",
                tone = TextSecondary,
                muted = true,
                onClick = {
                    vm.addReviewWithReason(typed)
                    query = ""
                    showAll = false
                    keyboard?.hide()
                },
            )
        }
    }
}

@Composable
internal fun TemplateRow(
    label: String,
    detail: String?,
    tone: Color,
    onClick: () -> Unit,
    hint: String? = null,
    muted: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .background(Surface1, RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (muted) Outline else tone.copy(alpha = 0.35f),
                    RoundedCornerShape(14.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(Modifier.padding(top = 5.dp).size(12.dp).background(tone, CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = if (muted) TextSecondary else MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = CardBody,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


/**
 * What the presentation is likely to be, and what to order — on the card, open,
 * where it will actually be read at 4 am.
 *
 * It sits above the ABCDE fields deliberately: the differential is what you
 * want in your head *before* you start writing, and putting it inside a text
 * field you have to scroll to is the same as not having it at all.
 */
@Composable
internal fun GuidancePanel(template: ReviewTemplate) {
    var open by rememberSaveable(template.label) { mutableStateOf(true) }
    val tone = priorityColor(template.priority)

    Column(
        Modifier
            .fillMaxWidth()
            .background(tone.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, tone.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { open = !open }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                template.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = tone,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (open) "Hide prompts" else "Show prompts",
                tint = tone,
            )
        }
        if (open) {
            Column(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (template.thinkAbout.isNotBlank()) {
                    GuidanceBlock("THINK ABOUT", template.thinkAbout, tone)
                }
                if (template.workupPrompt.isNotBlank()) {
                    GuidanceBlock("WORKUP", template.workupPrompt, tone)
                }
                Text(
                    "Prompts only. Nothing here is a finding, and none of it reaches your " +
                        "note until you type it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
internal fun GuidanceBlock(
    heading: String,
    body: String,
    tone: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(heading, style = MaterialTheme.typography.labelSmall, color = tone)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = CardBody)
    }
}

@Composable
internal fun CompletedReviewRow(
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
internal fun AbcdeField(
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
