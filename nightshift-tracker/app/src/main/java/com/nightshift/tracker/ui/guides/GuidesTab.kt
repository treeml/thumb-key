package com.nightshift.tracker.ui.guides

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.theme.DangerRed
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.TextSecondary

@Composable
fun GuidesTab() {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                GUIDES_DISCLAIMER,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Surface1, RoundedCornerShape(12.dp))
                        .border(1.dp, Outline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
            )
        }
        items(guides, key = { it.title }) { guide ->
            GuideCard(guide)
        }
    }
}

@Composable
private fun GuideCard(guide: Guide) {
    var expanded by rememberSaveable(guide.title) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(16.dp))
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .animateContentSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(guide.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    guide.oneLiner,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = TextSecondary,
            )
        }
        if (expanded) {
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                guide.sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            section.heading,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        section.lines.forEach { line -> Bullet(line, Color(0xFFD5DBE8)) }
                    }
                }
                // Red contraindications block — always last, always visible when open.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(DangerRed.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "⛔ CONTRAINDICATIONS / DO NOT",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DangerRed,
                    )
                    guide.contraindications.forEach { line -> Bullet(line, Color(0xFFF0C6C8)) }
                }
            }
        }
    }
}

@Composable
private fun Bullet(
    text: String,
    color: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = color, style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
