package com.nightshift.tracker.ui.handover

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ai.AiFactory
import com.nightshift.tracker.ui.AiState
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.reviews.copyToClipboard
import com.nightshift.tracker.ui.theme.DangerRed
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary

/**
 * The 08:00 screen. Everything here was already typed during the shift, so a
 * complete written handover costs one tap and a read-through.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandoverScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val text = vm.handoverText.collectAsStateValue()
    val shift = vm.activeShift.collectAsStateValue()
    val generation = vm.dataGeneration.collectAsStateValue()
    val aiState = vm.aiState.collectAsStateValue()
    val needsReview = vm.batchNeedsReview.collectAsStateValue()

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
                title = { Text("Handover") },
                actions = {
                    IconButton(onClick = { vm.regenerateHandover() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Regenerate")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Built from this shift's jobs, reviews and escalations. Edit freely — " +
                    "regenerating replaces your edits.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            if (shift != null) {
                DbTextField(
                    value = shift.handoverNote,
                    onCommit = { vm.setHandoverNote(it) },
                    label = "Watch out for (goes into the handover)",
                    seedKey = "handover-note-$generation",
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .background(Surface2, RoundedCornerShape(12.dp))
                        .clickable { vm.regenerateHandover() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Rebuild handover with this note",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (AiFactory.AVAILABLE) {
                NsAction(
                    label = if (aiState is AiState.Running) "Tidying…" else "Tidy with Claude",
                    onClick = { vm.tidyHandover() },
                    icon = Icons.Filled.AutoAwesome,
                    tone = MaterialTheme.colorScheme.primary,
                    filled = true,
                    enabled = aiState !is AiState.Running,
                    modifier = Modifier.fillMaxWidth(),
                )
                (aiState as? AiState.Error)?.let {
                    Text(it.message, style = MaterialTheme.typography.bodyMedium, color = DangerRed)
                }
            }
            if (needsReview) {
                NsAction(
                    label = "AI-tidied — tap when you've checked it",
                    onClick = { vm.markBatchReviewed() },
                    tone = DangerRed,
                    filled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { vm.editHandoverText(it) },
                label = { Text("Handover") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 14,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    HandoverAction("Copy", Icons.Filled.ContentCopy, MaterialTheme.colorScheme.primary) {
                        copyToClipboard(context, "Handover", text)
                        vm.noteCopied()
                    }
                }
                Box(Modifier.weight(1f)) {
                    HandoverAction("Share / email", Icons.Filled.Share, TextSecondary) {
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Handover — ${shift?.label.orEmpty()}")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                        context.startActivity(Intent.createChooser(send, "Share handover"))
                    }
                }
            }
            Text(
                "Identifiable clinical information — share to hospital systems only.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Box(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HandoverAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}
