package com.nightshift.tracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nightshift.tracker.ai.AiFactory
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.design.Radius
import com.nightshift.tracker.ui.design.SectionLabel
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.theme.Accent
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val left by AppSettings.leftHanded.collectAsStateWithLifecycle()
    val large by AppSettings.largeText.collectAsStateWithLifecycle()
    val night by AppSettings.nightVision.collectAsStateWithLifecycle()
    val haptics by AppSettings.haptics.collectAsStateWithLifecycle()
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                navigationIcon = {
                    IconButton(onClick = { vm.screen.value = Screen.Home }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
        ) {
            item { SectionLabel("HOW YOU HOLD IT") }
            item {
                HandChoice(left = left, onPick = { AppSettings.setLeftHanded(context, it) })
            }
            item {
                Text(
                    "Primary actions sit inside your thumb's arc and destructive ones sit " +
                        "outside it. Everything mirrors: the send key on the capture bar, " +
                        "the add buttons, the tick boxes on round cards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            item { SectionLabel("READABILITY") }
            item {
                Toggle(
                    title = "Larger text",
                    detail = "Scales the whole type ramp by 15%. Layouts stay put.",
                    checked = large,
                ) { AppSettings.setLargeText(context, it) }
            }
            item {
                Toggle(
                    title = "Night vision tint",
                    detail =
                        "Warm red overlay. Blue light is what wrecks dark adaptation, so " +
                            "this keeps your eyes usable when you look up from the screen " +
                            "into a dark corridor.",
                    checked = night,
                ) { AppSettings.setNightVision(context, it) }
            }
            item {
                Toggle(
                    title = "Haptic confirmation",
                    detail = "A tick on done, delete and capture — confirmation without looking.",
                    checked = haptics,
                ) { AppSettings.setHaptics(context, it) }
            }

            if (AiFactory.AVAILABLE) {
                item { SectionLabel("NOTE TIDYING") }
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .background(Surface1, RoundedCornerShape(Radius.md))
                            .border(1.dp, Outline, RoundedCornerShape(Radius.md))
                            .clickable { showKey = true }
                            .padding(Space.md),
                    ) {
                        Column {
                            Text(
                                if (vm.hasApiKey()) "Anthropic API key — set" else "Anthropic API key — not set",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (vm.hasApiKey()) Accent else TextSecondary,
                            )
                            Text(
                                "Used only when you tap Tidy. Identifiers are stripped before " +
                                    "the request and restored on this phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            item { SectionLabel("YOUR DATA") }
            item {
                Text(
                    "Everything lives in this app's private storage on this phone. Backups " +
                        "are written automatically after every change, and Export on the home " +
                        "screen writes a file you can keep anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }

    if (showKey) {
        var key by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showKey = false },
            title = { Text("Anthropic API key") },
            text = {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("sk-ant-…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setApiKey(key)
                    showKey = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showKey = false }) { Text("Cancel") } },
            containerColor = Surface1,
        )
    }
}

@Composable
private fun HandChoice(
    left: Boolean,
    onPick: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
        HandOption("Left-handed", left, Modifier.weight(1f)) { onPick(true) }
        HandOption("Right-handed", !left, Modifier.weight(1f)) { onPick(false) }
    }
}

@Composable
private fun HandOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .defaultMinSize(minHeight = 64.dp)
            .background(
                if (selected) Accent.copy(alpha = 0.16f) else Surface1,
                RoundedCornerShape(Radius.md),
            ).border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) Accent else Outline,
                RoundedCornerShape(Radius.md),
            ).clickable(onClick = onClick)
            .padding(Space.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Accent else TextSecondary,
        )
    }
}

@Composable
private fun Toggle(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(Radius.md))
                .clickable { onChange(!checked) }
                .padding(Space.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
