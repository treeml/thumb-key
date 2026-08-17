package com.nightshift.tracker.ui.shift

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nightshift.tracker.BuildConfig
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.guides.GuidesTab
import com.nightshift.tracker.ui.guides.UroLearnTab
import com.nightshift.tracker.ui.jobs.JobsTab
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.reviews.ReviewsTab
import com.nightshift.tracker.ui.rounds.RoundsTab
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Surface2

private val tabTitles =
    if (BuildConfig.URO) {
        listOf("Jobs", "Rounds", "Reviews", "Learn")
    } else {
        listOf("Jobs", "Reviews", "Guides")
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(vm: MainViewModel) {
    val shift = vm.activeShift.collectAsStateValue() ?: return
    val generation = vm.dataGeneration.collectAsStateValue()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(vm.snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                navigationIcon = {
                    IconButton(onClick = { vm.screen.value = Screen.Home }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                },
                title = { Text(shift.label) },
                actions = {
                    IconButton(onClick = { confirmArchive = true }) {
                        Icon(Icons.Filled.Archive, contentDescription = "Archive shift")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = Ink) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (tabTitles[tab]) {
                "Jobs" -> JobsTab(vm, generation)
                "Rounds" -> RoundsTab(vm, generation)
                "Reviews" -> ReviewsTab(vm, generation)
                "Learn" -> UroLearnTab()
                else -> GuidesTab()
            }
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archive this shift?") },
            text = { Text("The shift moves to the archive on the home screen. Jobs and reviews stay exactly as they are and remain searchable.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    vm.archiveActiveShift()
                }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } },
            containerColor = Surface2,
        )
    }
}
