package com.nightshift.tracker.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.data.Shift
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.archive.rememberArchiveSearch
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.theme.Accent
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val shiftDateFmt = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())

@Composable
fun HomeScreen(vm: MainViewModel) {
    val active = vm.activeShift.collectAsStateValue()
    val archived = vm.archivedShifts.collectAsStateValue()

    // Hoisted once for the whole screen: recompositions of the result list
    // never touch this state, so the keyboard stays up between keystrokes.
    var query by rememberSaveable { mutableStateOf("") }
    val searchResults = rememberArchiveSearch(vm, query)

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column {
                Text(
                    if (com.nightshift.tracker.BuildConfig.URO) "UroDay" else "Nightshift",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        item {
            if (active != null) {
                BigButton(
                    title = "Resume shift",
                    subtitle = "${active.label} — started ${shiftDateFmt.format(Date(active.startedAt))}",
                    accent = true,
                ) { vm.screen.value = Screen.ActiveShift }
            } else {
                var label by remember { mutableStateOf(defaultShiftLabel()) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Surface1, RoundedCornerShape(16.dp))
                        .border(1.dp, Outline, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Shift name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BigButton(title = "Start new shift", subtitle = null, accent = true) {
                        vm.startShift(label.ifBlank { defaultShiftLabel() })
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search archived shifts") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isNotBlank()) {
            items(searchResults, key = { "hit-${it.shift.id}" }) { hit ->
                ArchiveRow(
                    shift = hit.shift,
                    subtitle = hit.snippet,
                    vm = vm,
                )
            }
            if (searchResults.isEmpty()) {
                item {
                    Text(
                        "Nothing matches \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        } else {
            item {
                Text(
                    "Archived shifts",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            items(archived, key = { it.id }) { shift ->
                ArchiveRow(shift = shift, subtitle = null, vm = vm)
            }
            if (archived.isEmpty()) {
                item {
                    Text(
                        "No archived shifts yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

fun defaultShiftLabel(): String {
    val prefix = if (com.nightshift.tracker.BuildConfig.URO) "List " else "Night "
    return prefix + SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date())
}

@Composable
private fun BigButton(
    title: String,
    subtitle: String?,
    accent: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .background(
                if (accent) Accent.copy(alpha = 0.16f) else Surface1,
                RoundedCornerShape(16.dp),
            ).border(
                1.dp,
                if (accent) Accent.copy(alpha = 0.6f) else Outline,
                RoundedCornerShape(16.dp),
            ).clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = if (accent) Accent else MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun ArchiveRow(
    shift: Shift,
    subtitle: String?,
    vm: MainViewModel,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(14.dp))
                .border(1.dp, Outline, RoundedCornerShape(14.dp))
                .clickable { vm.screen.value = Screen.ArchiveDetail(shift.id) }
                .padding(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(shift.label, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle ?: shiftDateFmt.format(Date(shift.startedAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
            )
        }
        ArmedDeleteButton(onConfirmedDelete = { vm.deleteArchivedShiftWithUndo(shift) })
    }
}
