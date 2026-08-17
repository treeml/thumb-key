package com.nightshift.tracker.ui.rounds

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.WardRound
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.components.PriorityPicker
import com.nightshift.tracker.ui.jobs.CompletedDrawerHeader
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.reviews.copyToClipboard
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.priorityColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun buildRoundNote(round: WardRound): String {
    val now = Date()
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    return buildString {
        appendLine("UROLOGY WARD ROUND")
        appendLine("Date: $date  Time: $time")
        appendLine()
        appendLine("Patient: ${round.patientName.trim()} | Bed ${round.bed.trim()} | MRN: ${round.mrn.trim()}")
        if (round.dxOp.isNotBlank()) appendLine("Dx/Op: ${round.dxOp.trim()}")
        appendLine()
        if (round.overnight.isNotBlank()) {
            appendLine("Overnight:")
            appendLine(round.overnight.trim())
            appendLine()
        }
        if (round.exam.isNotBlank()) {
            appendLine("O/E:")
            appendLine(round.exam.trim())
            appendLine()
        }
        if (round.results.isNotBlank()) {
            appendLine("Results:")
            appendLine(round.results.trim())
            appendLine()
        }
        appendLine("Plan:")
        appendLine(round.plan.trim())
    }
}

@Composable
fun RoundsTab(
    vm: MainViewModel,
    generation: Int,
) {
    val rounds = vm.rounds.collectAsStateValue()
    val active = rounds.filter { !it.seen }
    val seen = rounds.filter { it.seen }
    var showSeen by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (rounds.isEmpty()) {
                item {
                    Text(
                        "No round entries yet. Add each patient before the round starts, " +
                            "fill in as you go, then mark them Seen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(active, key = { it.id }) { round ->
                RoundCard(round = round, vm = vm, generation = generation)
            }
            if (seen.isNotEmpty()) {
                item(key = "seen-drawer") {
                    CompletedDrawerHeader(
                        title = "Seen (${seen.size})",
                        expanded = showSeen,
                        onToggle = { showSeen = !showSeen },
                    )
                }
                if (showSeen) {
                    items(seen, key = { "seen-${it.id}" }) { round ->
                        SeenRoundRow(round = round, vm = vm)
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { vm.addRound() },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add patient") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

@Composable
private fun RoundCard(
    round: WardRound,
    vm: MainViewModel,
    generation: Int,
) {
    var expanded by rememberSaveable(round.id) { mutableStateOf(true) }
    val context = LocalContext.current
    val accent = priorityColor(round.priority)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(16.dp))
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .animateContentSize(),
    ) {
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
                val bedLabel = if (round.bed.isBlank()) "Bed —" else "Bed ${round.bed}"
                Text(
                    "$bedLabel  ·  ${round.patientName.ifBlank { "Unnamed" }}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (round.dxOp.isNotBlank()) {
                    Text(
                        round.dxOp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = if (expanded) 4 else 1,
                    )
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
                        value = round.bed,
                        onCommit = { vm.updateRound(round.copy(bed = it)) },
                        label = "Bed",
                        seedKey = "${round.id}-$generation-bed",
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
                    )
                    DbTextField(
                        value = round.patientName,
                        onCommit = { vm.updateRound(round.copy(patientName = it)) },
                        label = "Name",
                        seedKey = "${round.id}-$generation-name",
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    DbTextField(
                        value = round.mrn,
                        onCommit = { vm.updateRound(round.copy(mrn = it)) },
                        label = "MRN",
                        seedKey = "${round.id}-$generation-mrn",
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                    )
                }
                DbTextField(
                    value = round.dxOp,
                    onCommit = { vm.updateRound(round.copy(dxOp = it)) },
                    label = "Dx / operation (POD)",
                    seedKey = "${round.id}-$generation-dx",
                    modifier = Modifier.fillMaxWidth(),
                )
                DbTextField(
                    value = round.overnight,
                    onCommit = { vm.updateRound(round.copy(overnight = it)) },
                    label = "Overnight events",
                    seedKey = "${round.id}-$generation-on",
                    modifier = Modifier.fillMaxWidth(),
                )
                DbTextField(
                    value = round.exam,
                    onCommit = { vm.updateRound(round.copy(exam = it)) },
                    label = "O/E — obs, wound, drains, IDC (colour/output)",
                    seedKey = "${round.id}-$generation-exam",
                    modifier = Modifier.fillMaxWidth(),
                )
                DbTextField(
                    value = round.results,
                    onCommit = { vm.updateRound(round.copy(results = it)) },
                    label = "Results (bloods, imaging, histo)",
                    seedKey = "${round.id}-$generation-res",
                    modifier = Modifier.fillMaxWidth(),
                )
                DbTextField(
                    value = round.plan,
                    onCommit = { vm.updateRound(round.copy(plan = it)) },
                    label = "Plan",
                    seedKey = "${round.id}-$generation-plan",
                    modifier = Modifier.fillMaxWidth(),
                )
                PriorityPicker(selected = round.priority, onSelect = { vm.updateRound(round.copy(priority = it)) })

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ArmedDeleteButton(onConfirmedDelete = { vm.deleteRoundWithUndo(round) })
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .background(RoutineGreen.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                            .border(1.dp, RoutineGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { vm.markRoundSeen(round) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Seen ✓", style = MaterialTheme.typography.labelLarge, color = RoutineGreen)
                    }
                }

                if (round.plan.isNotBlank()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable {
                                copyToClipboard(context, "Ward round note", buildRoundNote(round))
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
                                "Generate round note → clipboard",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeenRoundRow(
    round: WardRound,
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
        Box(Modifier.size(12.dp).background(priorityColor(round.priority), CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                "Bed ${round.bed.ifBlank { "—" }} · ${round.patientName.ifBlank { "Unnamed" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
            )
            if (round.dxOp.isNotBlank()) {
                Text(
                    round.dxOp,
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
                .clickable { vm.reopenRound(round) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Restore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        ArmedDeleteButton(onConfirmedDelete = { vm.deleteRoundWithUndo(round) }, idleLabel = "Del")
    }
}
