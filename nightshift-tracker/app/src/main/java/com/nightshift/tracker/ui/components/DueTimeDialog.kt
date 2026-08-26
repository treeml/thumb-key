package com.nightshift.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.nightshift.tracker.ui.capture.parseClock
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.SectionLabel
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.design.spanText
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.UrgentRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One dialog for both ways of saying when something has to happen: "in forty
 * minutes" and "at 0400". They are the same thing underneath — an absolute
 * deadline with an alarm on it — so they belong in the same place rather than
 * in a "timer" and an "alarm" that behave differently.
 */
@Composable
fun DueTimeDialog(
    title: String,
    current: Long?,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)?,
    onPick: (Long) -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val resolved = parseClock(typed, now)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Space.md),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (current != null) {
                    val at = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(current))
                    val left = current - now
                    Text(
                        if (left <= 0) {
                            "Was due $at — overdue by ${spanText(-left)}."
                        } else {
                            "Currently due $at, in ${spanText(left)}."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (left <= 0) UrgentRed else TextSecondary,
                    )
                }

                SectionLabel("IN")
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(5, 10, 15, 20).forEach { m ->
                        NsAction("${m}m", { onPick(now + m * 60_000L) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(30, 45, 60, 120).forEach { m ->
                        NsAction(
                            if (m >= 60) "${m / 60}h" else "${m}m",
                            { onPick(now + m * 60_000L) },
                        )
                    }
                }

                HorizontalDivider()

                SectionLabel("AT A TIME")
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    placeholder = { Text("0400, 4:15, 6pm, midnight") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { resolved?.let(onPick) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (typed.isNotBlank()) {
                    Text(
                        if (resolved == null) {
                            "Not a time I recognise."
                        } else {
                            val at = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(resolved))
                            "$at — ${spanText(resolved - now)} from now."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                NsAction(
                    label = "Set",
                    onClick = { resolved?.let(onPick) },
                    tone = MaterialTheme.colorScheme.primary,
                    filled = true,
                    enabled = resolved != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            if (onClear != null) TextButton(onClick = onClear) { Text("Clear", color = UrgentRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = Surface2,
    )
}
