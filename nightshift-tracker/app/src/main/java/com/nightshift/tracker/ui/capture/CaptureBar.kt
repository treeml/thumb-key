package com.nightshift.tracker.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.design.Radius
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.design.rememberTick
import com.nightshift.tracker.ui.settings.leftHanded
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.priorityColor

/**
 * The front door of the app. One line — typed or dictated — becomes a job with
 * bed, priority and a running timer, without touching a single control.
 *
 * Writing up a paper round is the volume case: jobs arrive in clumps per bed.
 * So the bed can be PINNED once and every following line inherits it, and one
 * entry can hold several jobs separated by semicolons or new lines. A line
 * that names its own bed still overrides the pin.
 *
 * The chips underneath show what was understood BEFORE it commits, so the
 * parsing is never a silent guess.
 */
@Composable
fun CaptureBar(
    onCapture: (String) -> Unit,
    modifier: Modifier = Modifier,
    seed: String? = null,
    onSeedConsumed: () -> Unit = {},
    lockedBed: String = "",
    onBedChange: (String) -> Unit = {},
    knownBeds: List<String> = emptyList(),
) {
    var raw by remember { mutableStateOf("") }
    var showBedPicker by remember { mutableStateOf(false) }
    var newBed by remember { mutableStateOf("") }
    val parsed = remember(raw) { parseCapture(raw) }
    val focusRequester = remember { FocusRequester() }
    val tick = rememberTick()
    val left = leftHanded()

    LaunchedEffect(seed) {
        if (seed != null) {
            raw = seed
            onSeedConsumed()
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun submit() {
        if (raw.isNotBlank()) {
            tick()
            onCapture(raw)
            raw = ""
        }
    }

    val lineCount = raw.split('\n', ';').count { it.isNotBlank() }

    Column(
        modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            // The pin: tap to choose the bed these lines belong to.
            val pinTone =
                if (lockedBed.isBlank()) TextSecondary else MaterialTheme.colorScheme.primary
            Box(
                Modifier
                    .background(pinTone.copy(alpha = 0.14f), RoundedCornerShape(Radius.sm))
                    .border(1.dp, pinTone.copy(alpha = 0.5f), RoundedCornerShape(Radius.sm))
                    .clickable { showBedPicker = !showBedPicker }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    if (lockedBed.isBlank()) "Pin a bed" else "Bed $lockedBed ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = pinTone,
                )
            }
            if (raw.isNotBlank()) {
                if (lineCount > 1) {
                    Chip("$lineCount jobs", MaterialTheme.colorScheme.primary)
                }
                parsed.chips().forEach { chip ->
                    val tint =
                        when (chip) {
                            "URGENT" -> priorityColor(1)
                            "SOON" -> priorityColor(2)
                            "ROUTINE" -> priorityColor(3)
                            else -> TextSecondary
                        }
                    // With a bed pinned and none typed, show where it will land.
                    Chip(chip, tint)
                }
                if (parsed.bed.isBlank() && lockedBed.isNotBlank() && lineCount == 1) {
                    Chip("→ Bed $lockedBed", MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (showBedPicker) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    knownBeds.forEach { bed ->
                        val selected = bed == lockedBed
                        Box(
                            Modifier
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        Surface2
                                    },
                                    RoundedCornerShape(Radius.sm),
                                ).clickable {
                                    onBedChange(bed)
                                    showBedPicker = false
                                    runCatching { focusRequester.requestFocus() }
                                }.padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                bed,
                                style = MaterialTheme.typography.labelLarge,
                                color =
                                    if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    OutlinedTextField(
                        value = newBed,
                        onValueChange = { newBed = it },
                        placeholder = { Text("Other bed", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    onBedChange(newBed)
                                    newBed = ""
                                    showBedPicker = false
                                },
                            ),
                        modifier = Modifier.width(150.dp),
                    )
                    Box(
                        Modifier
                            .background(Surface2, RoundedCornerShape(Radius.sm))
                            .clickable {
                                onBedChange("")
                                showBedPicker = false
                            }.padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text("Unpin", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            val field: @Composable () -> Unit = {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it },
                    placeholder = {
                        Text(
                            if (lockedBed.isBlank()) {
                                "b12 chase K+ !1 30m"
                            } else {
                                "chase K+ ; order CT ; call family"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    maxLines = 4,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Outline,
                        ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            val sendKey: @Composable () -> Unit = {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(
                            if (raw.isBlank()) Surface2 else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(Radius.md),
                        ).clickable(enabled = raw.isNotBlank()) { submit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Add job",
                        tint = if (raw.isBlank()) TextSecondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            // The send key sits under the thumb: left edge for a left-hander.
            if (left) {
                sendKey()
                Box(Modifier.weight(1f)) { field() }
            } else {
                Box(Modifier.weight(1f)) { field() }
                sendKey()
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Box(
        Modifier
            .background(tint.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
