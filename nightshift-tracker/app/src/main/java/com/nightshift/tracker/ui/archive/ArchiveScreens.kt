package com.nightshift.tracker.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.data.Shift
import com.nightshift.tracker.data.WardRound
import com.nightshift.tracker.ui.ArchiveSearchHit
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.priorityColor
import com.nightshift.tracker.ui.theme.priorityLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberArchiveSearch(
    vm: MainViewModel,
    query: String,
): List<ArchiveSearchHit> {
    var results by remember { mutableStateOf(emptyList<ArchiveSearchHit>()) }
    LaunchedEffect(query) {
        results = if (query.isBlank()) emptyList() else vm.searchArchived(query)
    }
    return results
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDetailScreen(
    vm: MainViewModel,
    shiftId: String,
) {
    var shift by remember { mutableStateOf<Shift?>(null) }
    var jobs by remember { mutableStateOf(emptyList<Job>()) }
    var reviews by remember { mutableStateOf(emptyList<Review>()) }
    var rounds by remember { mutableStateOf(emptyList<WardRound>()) }
    LaunchedEffect(shiftId) {
        val detail = vm.archivedShiftDetail(shiftId)
        shift = detail.shift
        jobs = detail.jobs
        reviews = detail.reviews
        rounds = detail.rounds
    }
    val fmt = SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault())

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(vm.snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                navigationIcon = {
                    IconButton(onClick = { vm.screen.value = Screen.Home }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(shift?.label ?: "Archived shift") },
                actions = {
                    shift?.let { s ->
                        ArmedDeleteButton(
                            onConfirmedDelete = { vm.deleteArchivedShiftWithUndo(s) },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                shift?.let {
                    Text(
                        "Started ${fmt.format(Date(it.startedAt))}" +
                            (it.archivedAt?.let { a -> " · archived ${fmt.format(Date(a))}" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            item { SectionLabel("Jobs (${jobs.size})") }
            if (jobs.isEmpty()) item { EmptyLine("No jobs recorded.") }
            items(jobs.size) { i -> ArchivedJobCard(jobs[i]) }

            if (rounds.isNotEmpty()) {
                item { SectionLabel("Ward round (${rounds.size})") }
                items(rounds.size) { i -> ArchivedRoundCard(rounds[i]) }
            }

            item { SectionLabel("Reviews (${reviews.size})") }
            if (reviews.isEmpty()) item { EmptyLine("No reviews recorded.") }
            items(reviews.size) { i -> ArchivedReviewCard(reviews[i]) }

            item { Spacer(Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
}

@Composable
private fun ArchivedJobCard(job: Job) {
    val accent = priorityColor(job.priority)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(14.dp))
                .border(1.dp, Outline, RoundedCornerShape(14.dp))
                .padding(12.dp),
    ) {
        Box(Modifier.size(12.dp).background(accent, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                (if (job.bed.isNotBlank()) "Bed ${job.bed} — " else "") + job.text.ifBlank { "(handwritten / empty)" },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            if (job.status == 2) "DONE" else if (job.status == 1) "IN PROG" else "NOT DONE",
            style = MaterialTheme.typography.labelSmall,
            color = if (job.status == 2) RoutineGreen else TextSecondary,
        )
    }
}

@Composable
private fun ArchivedRoundCard(round: WardRound) {
    val accent = priorityColor(round.priority)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).background(accent, CircleShape))
            Text(
                "Bed ${round.bed.ifBlank { "—" }} · ${round.patientName.ifBlank { "Unnamed" }}" +
                    (if (round.mrn.isNotBlank()) " · MRN ${round.mrn}" else ""),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (round.seen) "SEEN" else "NOT SEEN",
                style = MaterialTheme.typography.labelSmall,
                color = if (round.seen) RoutineGreen else TextSecondary,
            )
        }
        if (round.dxOp.isNotBlank()) Field("Dx / Op", round.dxOp)
        if (round.overnight.isNotBlank()) Field("Overnight", round.overnight)
        if (round.exam.isNotBlank()) Field("O/E", round.exam)
        if (round.results.isNotBlank()) Field("Results", round.results)
        if (round.plan.isNotBlank()) Field("Plan", round.plan)
    }
}

@Composable
private fun ArchivedReviewCard(review: Review) {
    val accent = priorityColor(review.priority)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).background(accent, CircleShape))
            Text(
                "Bed ${review.bed.ifBlank { "—" }} · ${review.patientName.ifBlank { "Unnamed" }}" +
                    (if (review.mrn.isNotBlank()) " · MRN ${review.mrn}" else ""),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(priorityLabel(review.priority), style = MaterialTheme.typography.labelSmall, color = accent)
        }
        if (review.reason.isNotBlank()) Field("Reason", review.reason)
        listOf("A" to review.a, "B" to review.b, "C" to review.c, "D" to review.d, "E" to review.e)
            .filter { it.second.isNotBlank() }
            .forEach { (letter, value) -> Field(letter, value) }
        if (review.investigations.isNotBlank()) Field("Investigations", review.investigations)
        if (review.impression.isNotBlank()) Field("Impression", review.impression)
        if (review.plan.isNotBlank()) Field("Plan", review.plan)
        Text(
            if (review.registrarNotified) "Registrar notified ✓" else "Registrar not notified",
            style = MaterialTheme.typography.labelLarge,
            color = if (review.registrarNotified) RoutineGreen else TextSecondary,
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
