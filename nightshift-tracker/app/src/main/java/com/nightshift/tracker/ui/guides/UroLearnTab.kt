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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.theme.Accent
import com.nightshift.tracker.ui.theme.CardBody
import com.nightshift.tracker.ui.theme.DangerBody
import com.nightshift.tracker.ui.theme.DangerRed
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.TextSecondary

/**
 * UroDay's Learn tab: procedural tutorials first (the how-to knowledge a
 * urology JMO actually uses), then condition guides in the same format as
 * the Nightshift guides, each with a red never-miss section.
 */
@Composable
fun UroLearnTab() {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                URO_DISCLAIMER,
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
        item {
            Text(
                "TUTORIALS — WARD SKILLS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        items(uroTutorials, key = { "tut-${it.title}" }) { tutorial ->
            TutorialCard(tutorial)
        }
        item {
            Text(
                "CONDITION GUIDES",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        items(uroGuides, key = { "guide-${it.title}" }) { guide ->
            GuideCard(guide)
        }
    }
}

@Composable
private fun TutorialCard(tutorial: Tutorial) {
    var expanded by rememberSaveable(tutorial.title) { mutableStateOf(false) }
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
                Text(tutorial.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    tutorial.oneLiner,
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
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    tutorial.steps.forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Accent,
                            )
                            Text(step, style = MaterialTheme.typography.bodyMedium, color = CardBody)
                        }
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(DangerRed.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "⛔ PITFALLS / NEVER",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DangerRed,
                    )
                    tutorial.pitfalls.forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", color = DangerBody, style = MaterialTheme.typography.bodyMedium)
                            Text(line, style = MaterialTheme.typography.bodyMedium, color = DangerBody)
                        }
                    }
                }
            }
        }
    }
}
