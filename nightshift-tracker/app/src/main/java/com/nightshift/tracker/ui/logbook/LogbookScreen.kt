package com.nightshift.tracker.ui.logbook

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.COMMON_PROCEDURES
import com.nightshift.tracker.data.LearningItem
import com.nightshift.tracker.data.OUTCOMES
import com.nightshift.tracker.data.ProcedureLog
import com.nightshift.tracker.data.SUPERVISION_LEVELS
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.theme.Accent
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.SoonYellow
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val stamp = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
private val dayStamp = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

/**
 * The part of the app that grows with the user rather than resetting each
 * shift: what they've done with their hands, and what they promised
 * themselves they'd look up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val procedures = vm.procedures.collectAsStateValue()
    val questions = vm.learning.collectAsStateValue()
    var tab by rememberSaveable { mutableStateOf(0) }
    var pendingProcedure by remember { mutableStateOf<String?>(null) }
    var showNewQuestion by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = com.nightshift.tracker.ui.theme.Ink,
        snackbarHost = { SnackbarHost(vm.snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.nightshift.tracker.ui.theme.Ink),
                navigationIcon = {
                    IconButton(onClick = { vm.screen.value = Screen.Home }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Logbook") },
                actions = {
                    IconButton(onClick = {
                        val body =
                            if (tab == 0) exportProcedures(procedures) else exportQuestions(questions)
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    if (tab == 0) "Procedure logbook" else "Clinical questions",
                                )
                                putExtra(Intent.EXTRA_TEXT, body)
                            }
                        context.startActivity(Intent.createChooser(send, "Export"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Export")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SegButton("Procedures (${procedures.size})", tab == 0) { tab = 0 }
                SegButton("Questions (${questions.count { it.answeredAt == null }})", tab == 1) { tab = 1 }
            }

            if (tab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { ProcedureSummary(procedures) }
                    item {
                        Text(
                            "TAP TO LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    item {
                        FlowChips(COMMON_PROCEDURES) { name -> pendingProcedure = name }
                    }
                    item {
                        Text(
                            "HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    if (procedures.isEmpty()) {
                        item {
                            Text(
                                "Nothing logged yet. Two taps after each procedure builds the " +
                                    "record your portfolio and your term supervisor will ask for.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    items(procedures, key = { it.id }) { entry ->
                        ProcedureRow(entry, vm)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                                .background(Accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable { showNewQuestion = true }
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "+ Something I need to look up",
                                style = MaterialTheme.typography.labelLarge,
                                color = Accent,
                            )
                        }
                    }
                    if (questions.isEmpty()) {
                        item {
                            Text(
                                "Every shift throws up something you half-know. Park it here in " +
                                    "five seconds, close the loop when you're not exhausted.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    items(questions, key = { it.id }) { item ->
                        QuestionRow(item, vm)
                    }
                }
            }
        }
    }

    pendingProcedure?.let { name ->
        LogProcedureDialog(
            name = name,
            onDismiss = { pendingProcedure = null },
            onSave = { supervision, outcome, notes ->
                vm.logProcedure(name, supervision, outcome, notes)
                pendingProcedure = null
            },
        )
    }
    if (showNewQuestion) {
        NewQuestionDialog(
            onDismiss = { showNewQuestion = false },
            onSave = { q ->
                vm.addQuestion(q)
                showNewQuestion = false
            },
        )
    }
}

@Composable
private fun SegButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(
                if (selected) Accent.copy(alpha = 0.16f) else Surface2,
                RoundedCornerShape(12.dp),
            ).border(1.dp, if (selected) Accent else Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Accent else TextSecondary,
        )
    }
}

@Composable
private fun ProcedureSummary(entries: List<ProcedureLog>) {
    val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    val thisWeek = entries.count { it.performedAt >= weekAgo }
    val independent = entries.count { it.supervision == "Independent" || it.supervision == "Taught someone" }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Stat("${entries.size}", "logged")
        Stat("$thisWeek", "this week")
        Stat("$independent", "unsupervised")
    }
}

@Composable
private fun Stat(
    value: String,
    label: String,
) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun FlowChips(
    names: List<String>,
    onPick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { name ->
                    Box(
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .background(Surface1, RoundedCornerShape(12.dp))
                            .border(1.dp, Outline, RoundedCornerShape(12.dp))
                            .clickable { onPick(name) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProcedureRow(
    entry: ProcedureLog,
    vm: MainViewModel,
) {
    val tint =
        when (entry.outcome) {
            "Complication", "Failed" -> SoonYellow
            else -> RoutineGreen
        }
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
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${stamp.format(Date(entry.performedAt))} · ${entry.supervision} · ${entry.outcome}",
                style = MaterialTheme.typography.bodyMedium,
                color = tint,
            )
            if (entry.notes.isNotBlank()) {
                Text(entry.notes, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        ArmedDeleteButton(onConfirmedDelete = { vm.deleteProcedureWithUndo(entry) }, idleLabel = "Del")
    }
}

@Composable
private fun QuestionRow(
    item: LearningItem,
    vm: MainViewModel,
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var answer by remember(item.id, item.answer) { mutableStateOf(item.answer) }
    val answered = item.answeredAt != null
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (answered) RoutineGreen.copy(alpha = 0.4f) else Outline,
                RoundedCornerShape(12.dp),
            ).padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded },
            ) {
                Text(item.question, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (answered) "Answered ${stamp.format(Date(item.answeredAt!!))}" else "Open · tap to answer",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (answered) RoutineGreen else TextSecondary,
                )
            }
            IconButton(onClick = { vm.toggleQuestionStar(item) }) {
                Icon(
                    if (item.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Star",
                    tint = if (item.starred) SoonYellow else TextSecondary,
                )
            }
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
            ArmedDeleteButton(onConfirmedDelete = { vm.deleteQuestionWithUndo(item) })
        } else if (item.answer.isNotBlank()) {
            Text(item.answer, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
        }
    }
}

@Composable
private fun LogProcedureDialog(
    name: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var supervision by remember { mutableStateOf(SUPERVISION_LEVELS[3]) }
    var outcome by remember { mutableStateOf(OUTCOMES[0]) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Supervision", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                PickRow(SUPERVISION_LEVELS, supervision) { supervision = it }
                Text("Outcome", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                PickRow(OUTCOMES, outcome) { outcome = it }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(supervision, outcome, notes) }) { Text("Log it") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface1,
    )
}

@Composable
private fun PickRow(
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .background(
                                if (isSelected) Accent.copy(alpha = 0.18f) else Surface2,
                                RoundedCornerShape(10.dp),
                            ).border(
                                1.dp,
                                if (isSelected) Accent else Outline,
                                RoundedCornerShape(10.dp),
                            ).clickable { onPick(option) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            option,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Accent else TextSecondary,
                        )
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NewQuestionDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var question by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Look up later") },
        text = {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("The question") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(question) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface1,
    )
}

private fun exportProcedures(entries: List<ProcedureLog>): String =
    buildString {
        appendLine("PROCEDURE LOGBOOK")
        appendLine("Exported ${stamp.format(Date())} · ${entries.size} entries")
        appendLine()
        appendLine("Date,Procedure,Supervision,Outcome,Notes")
        entries.forEach { e ->
            appendLine(
                listOf(
                    dayStamp.format(Date(e.performedAt)),
                    e.name,
                    e.supervision,
                    e.outcome,
                    e.notes.replace(",", ";").replace("\n", " "),
                ).joinToString(","),
            )
        }
    }

private fun exportQuestions(items: List<LearningItem>): String =
    buildString {
        appendLine("CLINICAL QUESTIONS")
        appendLine("Exported ${stamp.format(Date())} · ${items.size} entries")
        appendLine()
        items.forEach { i ->
            appendLine("Q: ${i.question}")
            if (i.answer.isNotBlank()) appendLine("A: ${i.answer}")
            appendLine("   (${dayStamp.format(Date(i.createdAt))})")
            appendLine()
        }
    }
