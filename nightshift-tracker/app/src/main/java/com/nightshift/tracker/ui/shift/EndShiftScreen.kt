package com.nightshift.tracker.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.handover.shiftIssues
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.theme.DangerRed
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.SoonYellow
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary

/**
 * The end-of-shift safety net. It doesn't block anyone — it just makes sure
 * that anything being left behind is left behind on purpose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndShiftScreen(vm: MainViewModel) {
    val jobs = vm.jobs.collectAsStateValue()
    val reviews = vm.reviews.collectAsStateValue()
    val rounds = vm.rounds.collectAsStateValue()
    val issues = remember(jobs, reviews, rounds) { shiftIssues(jobs, reviews, rounds) }
    val blockers = issues.filter { it.severity == 1 }
    val notes = issues.filter { it.severity == 2 }
    var armed by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(vm.snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                navigationIcon = {
                    IconButton(onClick = { vm.screen.value = Screen.ActiveShift }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("End of shift") },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                if (issues.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(RoutineGreen.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                            .border(1.dp, RoutineGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Nothing outstanding",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoutineGreen,
                        )
                        Text(
                            "Every job is done, every review has an impression and a plan. Go home.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                } else {
                    Text(
                        "Before you archive this shift, a last look at what's being left behind.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            if (blockers.isNotEmpty()) {
                item { GroupLabel("NEEDS A DECISION (${blockers.size})", DangerRed) }
                items(blockers.size) { i -> IssueRow(blockers[i].label, blockers[i].detail, DangerRed) }
            }
            if (notes.isNotEmpty()) {
                item { GroupLabel("WORTH HANDING OVER (${notes.size})", SoonYellow) }
                items(notes.size) { i -> IssueRow(notes[i].label, notes[i].detail, SoonYellow) }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .background(Surface2, RoundedCornerShape(14.dp))
                        .clickable { vm.openHandover() }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Write the handover first →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .background(
                            if (armed) DangerRed else Surface1,
                            RoundedCornerShape(14.dp),
                        ).border(
                            1.dp,
                            if (armed) DangerRed else Outline,
                            RoundedCornerShape(14.dp),
                        ).clickable {
                            if (armed) vm.archiveActiveShift() else armed = true
                        }.padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (armed) "Tap again to archive and finish" else "Archive shift",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (armed) androidx.compose.ui.graphics.Color.White else TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun IssueRow(
    label: String,
    detail: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(12.dp))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
