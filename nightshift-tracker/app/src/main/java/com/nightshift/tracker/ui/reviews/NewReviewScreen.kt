package com.nightshift.tracker.ui.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.Screen
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.theme.Ink

/**
 * Starting a review: type the presentation, pick the closest match.
 *
 * On its own screen because picking one is a decision, not a glance — and
 * because the search box needs the keyboard and the whole screen for its
 * results rather than fighting the board for space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReviewScreen(vm: MainViewModel) {
    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                navigationIcon = {
                    IconButton(onClick = { vm.screen.value = Screen.ActiveShift }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to the board")
                    }
                },
                title = { Text("New review") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = Space.sm),
        ) {
            TemplateSearch(vm)
        }
    }
}
