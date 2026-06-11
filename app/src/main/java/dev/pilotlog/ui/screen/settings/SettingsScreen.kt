// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pilotlog.domain.model.DateFormat
import dev.pilotlog.domain.model.PilotRole
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dev.pilotlog.ui.component.AirportSearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onPreviousTotals: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    val jsonPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importLegacyJson(context, uri)
    }

    val exportFlightsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> if (uri != null) viewModel.exportFlights(context, uri) }

    val importFlightsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importFlights(context, uri) }

    val exportReferenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportReference(context, uri) }

    val importReferenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importReference(context, uri) }

    var pdfFromDate by remember { mutableStateOf<LocalDate?>(null) }
    var showPdfDateDialog by remember { mutableStateOf(false) }
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> if (uri != null) viewModel.exportLogbookPdf(context, uri, pdfFromDate) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
            SectionHeader("Profile")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = state.settings.pilotName,
                    onValueChange = viewModel::onPilotNameChange,
                    label = { Text("Your name (default PIC)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Default role",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleChoiceChip("PIC", PilotRole.PIC, state.settings.defaultRole, viewModel::onDefaultRoleChange)
                    RoleChoiceChip("F/O", PilotRole.COPILOT, state.settings.defaultRole, viewModel::onDefaultRoleChange)
                    RoleChoiceChip("Dual", PilotRole.DUAL, state.settings.defaultRole, viewModel::onDefaultRoleChange)
                    RoleChoiceChip("Instr", PilotRole.INSTRUCTOR, state.settings.defaultRole, viewModel::onDefaultRoleChange)
                }
                Spacer(Modifier.height(12.dp))
                AirportSearchField(
                    query = state.homeBaseQuery,
                    onQueryChange = viewModel::onHomeBaseQueryChange,
                    suggestions = state.homeBaseSuggestions,
                    onAirportSelected = viewModel::onHomeBaseSelected,
                    label = "Home base",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Date format",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = state.settings.dateFormat == fmt,
                            onClick = { viewModel.onDateFormatChange(fmt) },
                            label = { Text(fmt.label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader("Logbook")

            NavigationRow(
                title = "Previous totals",
                subtitle = "Hours flown before this logbook started",
                onClick = onPreviousTotals,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader("Backup & restore")

            BackupRow(
                title = "Export logbook (PDF)",
                subtitle = "BCAA layout, page breaks honoured, totals per page",
                buttonText = "Export",
                busy = state.isBackupBusy,
                onClick = { showPdfDateDialog = true },
            )
            BackupRow(
                title = "Export flights (CSV)",
                subtitle = "Save all logged flights to a .csv file",
                buttonText = "Export",
                busy = state.isBackupBusy,
                onClick = { exportFlightsLauncher.launch("pilotlog-flights.csv") },
            )
            BackupRow(
                title = "Import flights (CSV)",
                subtitle = "Restore flights from a .csv backup",
                buttonText = "Import",
                busy = state.isBackupBusy,
                onClick = { importFlightsLauncher.launch(arrayOf("text/*", "text/csv", "*/*")) },
            )
            BackupRow(
                title = "Export setup (JSON)",
                subtitle = "Custom airports, aircraft, previous totals & settings",
                buttonText = "Export",
                busy = state.isBackupBusy,
                onClick = { exportReferenceLauncher.launch("pilotlog-setup.json") },
            )
            BackupRow(
                title = "Import setup (JSON)",
                subtitle = "Restore airports, aircraft, previous totals & settings",
                buttonText = "Import",
                busy = state.isBackupBusy,
                onClick = { importReferenceLauncher.launch(arrayOf("application/json", "*/*")) },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader("Maintenance")

            BackupRow(
                title = "Clear all flights",
                subtitle = "Delete every flight (e.g. before a clean re-import). Aircraft kept.",
                buttonText = "Clear",
                busy = state.isBackupBusy,
                onClick = { showClearConfirm = true },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader("Data")

            SettingsRow(
                title = "Import from flightlogbook backup",
                subtitle = "Pick a DB_export_FLIGHTLOGBOOK.txt file",
                trailing = {
                    if (state.isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        OutlinedButton(
                            onClick = { jsonPicker.launch(arrayOf("application/json", "*/*")) },
                        ) {
                            Icon(
                                Icons.Filled.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Pick file")
                        }
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }

    // Success dialog
    state.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::clearResult,
            title = { Text("Import complete") },
            text = {
                Text(
                    "${result.flightsImported} flights imported\n" +
                        "${result.aircraftImported} new aircraft added\n" +
                        "${result.skipped} entries skipped",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::clearResult) { Text("OK") }
            },
        )
    }

    // Error dialog
    state.importError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearResult,
            title = { Text("Import failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = viewModel::clearResult) { Text("OK") }
            },
        )
    }

    // Backup result / error dialog
    state.backupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearBackupResult,
            title = { Text("Done") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearBackupResult) { Text("OK") }
            },
        )
    }
    state.backupError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearBackupResult,
            title = { Text("Operation failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = viewModel::clearBackupResult) { Text("OK") }
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete all flights?") },
            text = {
                Text(
                    "This permanently removes every flight from the logbook. Aircraft types, " +
                        "registrations and settings are kept. Export a backup first if unsure.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAllFlightsAction()
                }) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showPdfDateDialog) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPdfDateDialog = false },
            confirmButton = {
                TextButton(
                    enabled = dateState.selectedDateMillis != null,
                    onClick = {
                        pdfFromDate = dateState.selectedDateMillis?.let {
                            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                        }
                        showPdfDateDialog = false
                        exportPdfLauncher.launch("yapl-logbook.pdf")
                    },
                ) { Text("From this date") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pdfFromDate = null
                    showPdfDateDialog = false
                    exportPdfLauncher.launch("yapl-logbook.pdf")
                }) { Text("All pages") }
            },
        ) {
            DatePicker(
                state = dateState,
                title = {
                    Text(
                        "Export from page of date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

@Composable
private fun BackupRow(
    title: String,
    subtitle: String,
    buttonText: String,
    busy: Boolean,
    onClick: () -> Unit,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                OutlinedButton(onClick = onClick) { Text(buttonText) }
            }
        },
    )
}

@Composable
private fun RoleChoiceChip(
    label: String,
    role: PilotRole,
    selected: PilotRole,
    onSelect: (PilotRole) -> Unit,
) {
    FilterChip(
        selected = role == selected,
        onClick = { onSelect(role) },
        label = { Text(label) },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun NavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        trailing()
    }
}
