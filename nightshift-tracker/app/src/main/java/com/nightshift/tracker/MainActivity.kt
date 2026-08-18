package com.nightshift.tracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.archive.ArchiveDetailScreen
import com.nightshift.tracker.ui.home.HomeScreen
import com.nightshift.tracker.ui.rounds.BatchNotesScreen
import com.nightshift.tracker.ui.shift.ShiftScreen
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.NightshiftTheme
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.URO) {
            // Light theme regardless of system dark mode: force dark status icons.
            val transparent = android.graphics.Color.TRANSPARENT
            enableEdgeToEdge(
                statusBarStyle = androidx.activity.SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = androidx.activity.SystemBarStyle.light(transparent, transparent),
            )
        } else {
            enableEdgeToEdge()
        }
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            NightshiftTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(vm: MainViewModel = viewModel()) {
    val screen by vm.screen.collectAsStateWithLifecycle()

    when (val s = screen) {
        is Screen.ArchiveDetail -> ArchiveDetailScreen(vm, s.shiftId)
        Screen.BatchNotes -> BatchNotesScreen(vm)
        Screen.ActiveShift -> ShiftScreen(vm)
        Screen.Home -> HomeShell(vm)
    }
}

/**
 * Home wraps the home screen plus the permanently visible backup bar —
 * Export backup is never more than one glance away.
 */
@Composable
private fun HomeShell(vm: MainViewModel) {
    val exportLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri -> uri?.let { vm.exportBackup(it) } }
    val importLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> uri?.let { vm.importBackup(it) } }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(vm.snackbarHostState) },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Ink)
                        .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 18.dp),
            ) {
                BackupBarButton(
                    label = "Export backup",
                    icon = { Icon(Icons.Filled.Upload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                ) {
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                    exportLauncher.launch("nightshift-backup-$stamp.json")
                }
                BackupBarButton(
                    label = "Import / restore",
                    icon = { Icon(Icons.Filled.Download, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                ) {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            HomeScreen(vm)
        }
    }
}

@Composable
private fun BackupBarButton(
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .defaultMinSize(minHeight = 52.dp)
            .background(Surface2, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
