package com.dessalines.thumbkey.ui.components.keyboard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Three-slot suggestion bar:
 *   [best correction] | [original word — bold, always tappable] | [second correction]
 *
 * Tapping a correction replaces the current word. Tapping the middle slot
 * commits the original word as-is (overriding any pending autocorrect).
 */
@Composable
fun SuggestionBar(
    suggestions: List<String>,
    currentWord: String,
    onSuggestionClick: (String) -> Unit,
) {
    // Build exactly 3 display slots: correction | original | correction
    val slots = listOf(
        suggestions.getOrNull(0) ?: "",
        currentWord,
        suggestions.getOrNull(1) ?: "",
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            slots.forEachIndexed { index, slot ->
                if (slot.isNotEmpty()) {
                    TextButton(
                        onClick = { onSuggestionClick(slot) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = slot,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // Middle slot (original word) shown bold so the user knows it's the override
                            fontWeight = if (index == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                } else {
                    // Empty spacer so layout stays balanced
                    TextButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    ) {}
                }
            }
        }
    }
}
