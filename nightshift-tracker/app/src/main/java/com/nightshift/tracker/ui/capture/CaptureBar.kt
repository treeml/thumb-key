package com.nightshift.tracker.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
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
 * The chips underneath show what was understood BEFORE it commits, so the
 * parsing is never a silent guess.
 */
@Composable
fun CaptureBar(
    onCapture: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var raw by remember { mutableStateOf("") }
    val parsed = remember(raw) { parseCapture(raw) }

    val tick = rememberTick()
    val left = leftHanded()

    fun submit() {
        if (raw.isNotBlank()) {
            tick()
            onCapture(raw)
            raw = ""
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (raw.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                parsed.chips().forEach { chip ->
                    val tint =
                        when (chip) {
                            "URGENT" -> priorityColor(1)
                            "SOON" -> priorityColor(2)
                            "ROUTINE" -> priorityColor(3)
                            else -> TextSecondary
                        }
                    Box(
                        Modifier
                            .background(tint.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(chip, style = MaterialTheme.typography.labelSmall, color = tint)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val field: @Composable () -> Unit = {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it },
                    placeholder = {
                        Text(
                            "b12 chase K+ !1 30m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Outline,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val sendKey: @Composable () -> Unit = {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(
                            if (raw.isBlank()) Surface2 else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp),
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
