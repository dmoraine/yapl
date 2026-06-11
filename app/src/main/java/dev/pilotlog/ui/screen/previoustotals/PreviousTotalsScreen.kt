// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.previoustotals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousTotalsScreen(
    onNavigateUp: () -> Unit,
    viewModel: PreviousTotalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbar.showSnackbar("Previous totals saved")
            onNavigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Previous Totals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::save) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SectionLabel("Reference date")
            OutlinedTextField(
                value = state.asOf,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.AS_OF, it) },
                label = { Text("As of date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            SectionLabel("Flight time (H:MM)")

            TimeRow(
                label = "Total",
                value = state.totalTime,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.TOTAL, it) },
            )
            TimeRow(
                label = "Night",
                value = state.nightTime,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.NIGHT, it) },
            )
            TimeRow(
                label = "IFR",
                value = state.ifrTime,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.IFR, it) },
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            SectionLabel("Role time (H:MM)")

            TimeRow(
                label = "PIC",
                value = state.picTime,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.PIC, it) },
            )
            TimeRow(
                label = "Co-pilot",
                value = state.copilotTime,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.COPILOT, it) },
            )
            TimeRow(
                label = "Dual",
                value = state.dualTime,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.DUAL, it) },
            )
            TimeRow(
                label = "Instructor",
                value = state.instructorTime,
                onValueChange = { viewModel.onFieldChange(PreviousTotalsField.INSTRUCTOR, it) },
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            SectionLabel("Operations")

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.landingsDay,
                    onValueChange = { viewModel.onFieldChange(PreviousTotalsField.LANDINGS_DAY, it) },
                    label = { Text("Landings (day)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = state.landingsNight,
                    onValueChange = { viewModel.onFieldChange(PreviousTotalsField.LANDINGS_NIGHT, it) },
                    label = { Text("Landings (night)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        // Text keyboard so a negative reconciliation value can be typed.
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.takeoffs,
                    onValueChange = { viewModel.onFieldChange(PreviousTotalsField.TAKEOFFS, it) },
                    label = { Text("Takeoffs") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.width(12.dp))
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Day/night landings seed the PDF's per-page landing totals. " +
                    "A value may be negative to reconcile an imperfect import against your paper logbook.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Padding below FAB
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun TimeRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("0:00") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        singleLine = true,
    )
}
