package com.nightshift.tracker.ui.learn

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.LearningItem
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.Radius
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.theme.Accent
import com.nightshift.tracker.ui.theme.CardBody
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.TextSecondary

/**
 * "I'll look that up later."
 *
 * Every shift throws up something you half-know. Writing it down has to cost
 * about three seconds or it never happens, and it has to be somewhere you'll
 * actually pass again — so the list lives in the Guides/Learn tab you already
 * open, and capture is one tap from the shift's top bar.
 */
@Composable
fun QuestionCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var question by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Look up later") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(
                    "Park it now, close the loop when you're not exhausted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("The question") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (question.isNotBlank()) onSave(question) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface1,
    )
}

/** The research list, rendered inside the Guides / Learn tab. */
@Composable
fun ResearchSection(vm: MainViewModel) {
    val items = vm.learning.collectAsStateValue()
    val open = items.filter { it.answeredAt == null }
    val answered = items.filter { it.answeredAt != null }
    var showAdd by remember { mutableStateOf(false) }
    var showAnswered by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "TO RESEARCH (${open.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            NsAction("+ Add", { showAdd = true }, tone = Accent, filled = true)
        }

        if (open.isEmpty()) {
            Text(
                "Nothing parked. Anything you half-knew tonight goes here — " +
                    "the bookmark button in the shift bar adds one in three seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        open.take(6).forEach { item -> QuestionCard(item, vm) }
        if (open.size > 6) {
            Text(
                "+ ${open.size - 6} more in the logbook",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        if (answered.isNotEmpty()) {
            Text(
                if (showAnswered) "Hide answered (${answered.size})" else "Answered (${answered.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showAnswered = !showAnswered }.padding(vertical = 4.dp),
            )
            if (showAnswered) {
                answered.take(10).forEach { item -> QuestionCard(item, vm) }
            }
        }

        NsAction(
            label = "Open the full logbook",
            onClick = { vm.screen.value = Screen.Logbook },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showAdd) {
        QuestionCaptureDialog(
            onDismiss = { showAdd = false },
            onSave = {
                vm.addQuestion(it)
                showAdd = false
            },
        )
    }
}

@Composable
private fun QuestionCard(
    item: LearningItem,
    vm: MainViewModel,
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var answer by remember(item.id, item.answer) { mutableStateOf(item.answer) }
    val answered = item.answeredAt != null

    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(Radius.md))
            .border(
                1.dp,
                if (answered) RoutineGreen.copy(alpha = 0.4f) else Outline,
                RoundedCornerShape(Radius.md),
            ).clickable { expanded = !expanded }
            .padding(Space.md)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Text(item.question, style = MaterialTheme.typography.bodyLarge)
        if (!expanded && item.answer.isNotBlank()) {
            Text(
                item.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = CardBody,
                maxLines = 2,
            )
        }
        if (expanded) {
            OutlinedTextField(
                value = answer,
                onValueChange = {
                    answer = it
                    vm.answerQuestion(item, it)
                },
                label = { Text("What you found out") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                NsAction("Delete", { vm.deleteQuestionWithUndo(item) })
                Box(Modifier.weight(1f))
                NsAction(
                    label = if (item.starred) "★ Starred" else "☆ Star",
                    onClick = { vm.toggleQuestionStar(item) },
                )
            }
        }
    }
}
