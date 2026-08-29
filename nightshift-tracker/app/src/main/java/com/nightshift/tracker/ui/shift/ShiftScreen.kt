package com.nightshift.tracker.ui.shift

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nightshift.tracker.BuildConfig
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.guides.GuidesTab
import com.nightshift.tracker.ui.guides.UroLearnTab
import com.nightshift.tracker.ui.jobs.BoardTab
import com.nightshift.tracker.ui.jobs.collectAsStateValue
import com.nightshift.tracker.ui.learn.QuestionCaptureDialog
import com.nightshift.tracker.ui.rounds.RoundsTab
import com.nightshift.tracker.ui.theme.Ink

// Centre first, and it is where you land: the board answers "show me
// everything", the centre answers "what now", and the second question is the
// one you actually ask when you pick the phone up.
private val tabTitles =
    if (BuildConfig.URO) {
        listOf("Centre", "Board", "Rounds", "Learn")
    } else {
        listOf("Centre", "Board", "Guides")
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(vm: MainViewModel) {
    val shift = vm.activeShift.collectAsStateValue() ?: return
    val generation = vm.dataGeneration.collectAsStateValue()
    // Held in the ViewModel so other surfaces can jump here (e.g. "+ Job" on a
    // ward round card lands on Jobs with the bed already typed).
    val tab = vm.activeTab.collectAsStateValue().coerceIn(0, tabTitles.lastIndex)
    var showResearchCapture by rememberSaveable { mutableStateOf(false) }

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
                    IconButton(onClick = { showResearchCapture = true }) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = "Look up later")
                    }
                    IconButton(onClick = { vm.openHandover() }) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Handover")
                    }
                    IconButton(onClick = { vm.openEndShift() }) {
                        Icon(Icons.Filled.DoneAll, contentDescription = "End shift")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Edge-to-edge means the keyboard is an inset, not a resize:
                // without this the capture bar hides behind it. Consuming the
                // scaffold insets first stops the nav bar being counted twice,
                // so the bar sits flush on the keyboard.
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            TabRow(selectedTabIndex = tab, containerColor = Ink) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { vm.selectTab(index) },
                        text = { Text(title) },
                    )
                }
            }
            when (tabTitles[tab]) {
                "Centre" -> CommandCentre(vm, onOpenBoard = { vm.selectTab(vm.BOARD_TAB) })
                "Board" -> BoardTab(vm, generation)
                "Rounds" -> RoundsTab(vm, generation)
                "Learn" -> UroLearnTab(vm)
                else -> GuidesTab(vm)
            }
        }
    }

    if (showResearchCapture) {
        QuestionCaptureDialog(
            onDismiss = { showResearchCapture = false },
            onSave = {
                vm.addQuestion(it)
                showResearchCapture = false
            },
        )
    }
}
