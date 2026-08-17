package com.nightshift.tracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.theme.DangerRed
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import com.nightshift.tracker.ui.theme.priorityColor
import kotlinx.coroutines.delay

/**
 * Text field whose committed value lives in Room. Local state keeps the
 * cursor stable while typing; every change is pushed straight to the DB.
 * [seedKey] re-seeds from the DB after import/undo (dataGeneration).
 * Sentence capitalization + no input filtering keeps voice-to-text happy.
 */
@Composable
fun DbTextField(
    value: String,
    onCommit: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    seedKey: Any = Unit,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    var local by remember(seedKey) { mutableStateOf(value) }
    OutlinedTextField(
        value = local,
        onValueChange = {
            local = it
            onCommit(it)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = TextSecondary,
            ),
    )
}

/**
 * Two-tap delete: first tap arms it (turns red, "Are you sure?") for 3 s,
 * second tap while armed actually deletes. Min 48 dp touch target.
 */
@Composable
fun ArmedDeleteButton(
    onConfirmedDelete: () -> Unit,
    modifier: Modifier = Modifier,
    idleLabel: String = "Delete",
) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(3000)
            armed = false
        }
    }
    val bg by animateColorAsState(
        targetValue = if (armed) DangerRed else Surface2,
        label = "deleteBg",
    )
    val fg = if (armed) Color.White else TextSecondary
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 48.dp)
                .background(bg, RoundedCornerShape(12.dp))
                .clickable {
                    if (armed) {
                        armed = false
                        onConfirmedDelete()
                    } else {
                        armed = true
                    }
                }.padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (armed) "Are you sure?" else idleLabel,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
        )
    }
}

/** Solid priority dot; also used as a large tap target when [onClick] set. */
@Composable
fun PriorityDot(
    priority: Int,
    modifier: Modifier = Modifier,
    size: Int = 14,
) {
    Box(
        modifier
            .size(size.dp)
            .background(priorityColor(priority), CircleShape),
    )
}

/** Row of the three priority levels as selectable pills. */
@Composable
fun PriorityPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1 to "1 URGENT", 2 to "2 SOON", 3 to "3 ROUTINE").forEach { (level, label) ->
            val color = priorityColor(level)
            val isSelected = selected == level
            Box(
                modifier =
                    Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .background(
                            if (isSelected) color.copy(alpha = 0.22f) else Surface2,
                            RoundedCornerShape(10.dp),
                        ).border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) color else Outline,
                            shape = RoundedCornerShape(10.dp),
                        ).clickable { onSelect(level) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) color else TextSecondary,
                )
            }
        }
    }
}
