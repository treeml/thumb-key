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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
    /** Label of the bed currently open, or null when none is. */
    targetLabel: String? = null,
    /** Bed labels already on this shift, so typing one can jump to it. */
    knownBeds: List<String> = emptyList(),
    /** Called instead of onCapture when the whole line is just a bed. */
    onJump: (String) -> Unit = {},
) {
    var raw by remember { mutableStateOf("") }
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

    /**
     * A line that is nothing but the name of a bed you already have is not a
     * job called "34" — it is you looking for bed 34. Typing it and pressing
     * send jumps to that patient instead of filing an empty task against them.
     *
     * Only ever an exact match against a bed that exists: guessing here would
     * mean a real job silently turning into a navigation, which is worse than
     * having to tap the list.
     */
    val jumpTarget =
        remember(raw, knownBeds) {
            val typed = raw.trim()
            if (typed.isBlank() || typed.contains(' ')) {
                null
            } else {
                val bare = typed.removePrefix("b").removePrefix("B")
                knownBeds.firstOrNull { it.equals(typed, true) || it.equals(bare, true) }
            }
        }

    fun submit() {
        if (raw.isBlank()) return
        tick()
        val jump = jumpTarget
        if (jump != null) {
            onJump(jump)
        } else {
            onCapture(raw)
        }
        raw = ""
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
            // Where these words are going. No mode to set: it follows the last
            // bed you wrote to, so a run of jobs for one patient only needs the
            // bed on the first line.
            Text(
                if (targetLabel.isNullOrBlank()) "No bed yet" else "→ ${bedLabel(targetLabel)}",
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (targetLabel.isNullOrBlank()) {
                        TextSecondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
            if (jumpTarget != null) {
                Chip("go to ${bedLabel(jumpTarget)}", MaterialTheme.colorScheme.primary)
            } else if (raw.isNotBlank()) {
                if (lineCount > 1) Chip("$lineCount jobs", MaterialTheme.colorScheme.primary)
                parsed.chips().forEach { chip ->
                    val tint =
                        when (chip) {
                            "URGENT" -> priorityColor(1)
                            "SOON" -> priorityColor(2)
                            "ROUTINE" -> priorityColor(3)
                            else -> TextSecondary
                        }
                    Chip(chip, tint)
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
                            if (targetLabel.isNullOrBlank()) {
                                "b56 MB 122484 chase potassium 0400"
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
                        if (jumpTarget != null) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (jumpTarget != null) "Go to bed" else "Add job",
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
