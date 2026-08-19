package com.nightshift.tracker.ui.rounds

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ai.AiFactory
import com.nightshift.tracker.ui.AiState
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.reviews.copyToClipboard
import com.nightshift.tracker.ui.theme.CardBody
import com.nightshift.tracker.ui.theme.DangerBody
import com.nightshift.tracker.ui.theme.DangerRed
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.SoonYellow
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchNotesScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val text = vm.batchText.collectAsStateValue()
    val aiState = vm.aiState.collectAsStateValue()
    val needsReview = vm.batchNeedsReview.collectAsStateValue()
    val subject = vm.batchSubject.collectAsStateValue()
    var showKeyDialog by remember { mutableStateOf(false) }

    val running = aiState is AiState.Running
    val blocked = needsReview

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
                title = { Text(subject) },
                actions = {
                    if (AiFactory.AVAILABLE) {
                        IconButton(onClick = { showKeyDialog = true }) {
                            Icon(Icons.Filled.Key, contentDescription = "AI settings")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Banner(
                color = SoonYellow,
                title = "Before you send",
                body =
                    "These notes contain patient identifiers. Send them to your hospital " +
                        "email account only, and paste into the DHR from there. Never send " +
                        "identifiable notes to a personal address.",
            )

            if (needsReview) {
                Banner(
                    color = DangerRed,
                    title = "AI-tidied — check every line",
                    body =
                        "Claude reformatted this text. Read it against what you actually " +
                            "found before it goes anywhere: you are signing it, not the model.",
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .background(DangerRed.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .clickable { vm.markBatchReviewed() }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "I have read and checked these notes",
                        style = MaterialTheme.typography.labelLarge,
                        color = DangerRed,
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { vm.editBatchText(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 12,
            )

            if (AiFactory.AVAILABLE) {
                ActionButton(
                    label = if (running) "Tidying…" else "Tidy with Claude",
                    icon = { tint ->
                        if (running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = tint,
                            )
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, tint = tint, modifier = Modifier.size(18.dp))
                        }
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    enabled = !running,
                ) {
                    if (vm.hasApiKey()) vm.tidyCurrentNote() else showKeyDialog = true
                }
                Text(
                    "Names, MRNs and bed numbers are replaced with placeholders before the " +
                        "request and restored on this phone afterwards — identifiers never " +
                        "leave the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            (aiState as? AiState.Error)?.let {
                Text(
                    it.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerRed,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    ActionButton(
                        label = "Copy all",
                        icon = { tint ->
                            Icon(Icons.Filled.ContentCopy, null, tint = tint, modifier = Modifier.size(18.dp))
                        },
                        tint = TextSecondary,
                        enabled = !blocked,
                    ) {
                        copyToClipboard(context, "Ward round notes", text)
                        vm.noteCopied()
                    }
                }
                Box(Modifier.weight(1f)) {
                    ActionButton(
                        label = "Email notes",
                        icon = { tint ->
                            Icon(Icons.Filled.Email, null, tint = tint, modifier = Modifier.size(18.dp))
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        enabled = !blocked,
                    ) {
                        val date = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date())
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "$subject — $date")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                        context.startActivity(Intent.createChooser(send, "Email notes"))
                    }
                }
            }
            if (blocked) {
                Text(
                    "Confirm you've checked the tidied notes to enable copy and email.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Box(Modifier.size(24.dp))
        }
    }

    if (showKeyDialog) {
        ApiKeyDialog(
            initial = "",
            onDismiss = { showKeyDialog = false },
            onSave = {
                vm.setApiKey(it)
                showKeyDialog = false
            },
        )
    }
}

@Composable
private fun Banner(
    color: Color,
    title: String,
    body: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = if (color == DangerRed) DangerBody else CardBody,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: @Composable (Color) -> Unit,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val effective = if (enabled) tint else TextSecondary.copy(alpha = 0.5f)
    Box(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .background(
                if (enabled) effective.copy(alpha = 0.14f) else Surface2,
                RoundedCornerShape(14.dp),
            ).border(1.dp, effective.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon(effective)
            Text(label, style = MaterialTheme.typography.labelLarge, color = effective)
        }
    }
}

@Composable
private fun ApiKeyDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var key by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anthropic API key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Paste a key from console.anthropic.com. It is stored only in this " +
                        "app's private storage on this phone and is used solely for the " +
                        "note-tidy request.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("sk-ant-…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(key) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface1,
    )
}
