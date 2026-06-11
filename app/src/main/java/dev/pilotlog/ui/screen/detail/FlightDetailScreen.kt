// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.ui.util.LocalDateFormat
import dev.pilotlog.ui.util.formatted
import dev.pilotlog.ui.util.toHhMm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: FlightDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.flight?.let { flight ->
                        IconButton(onClick = { onEdit(flight.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.flight == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { Text("Flight not found") }

            else -> FlightDetail(
                flight = state.flight!!,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete flight?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FlightDetail(flight: Flight, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Route header
        Text(
            text = "${flight.departureAirport}  →  ${flight.arrivalAirport}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = flight.date.formatted(LocalDateFormat.current),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        DetailRow("Departure", "${flight.departureAirport}  ${"%02d:%02d".format(flight.departureTime.hour, flight.departureTime.minute)} UTC")
        DetailRow("Arrival",   "${flight.arrivalAirport}  ${"%02d:%02d".format(flight.arrivalTime.hour, flight.arrivalTime.minute)} UTC")
        DetailRow("Aircraft",  "${flight.aircraftRegistration.ifBlank { "—" }}  ${flight.aircraftType}")
        if (flight.flightNumber.isNotBlank()) DetailRow("Flight", flight.flightNumber)

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TimeStatCol("Total",      flight.totalMinutes,      Modifier.weight(1f))
            TimeStatCol("Night",      flight.nightMinutes,      Modifier.weight(1f))
            TimeStatCol("IFR",        flight.ifrMinutes,        Modifier.weight(1f))
        }

        if (flight.isMultiPilot) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TimeStatCol("PIC",       flight.picMinutes,        Modifier.weight(1f))
                TimeStatCol("Copilot",   flight.copilotMinutes,    Modifier.weight(1f))
                TimeStatCol("Dual",      flight.dualMinutes,       Modifier.weight(1f))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column {
                Text("T/O", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${flight.takeoffsDay}D  ${flight.takeoffsNight}N", style = MaterialTheme.typography.bodyMedium)
            }
            Column {
                Text("LDG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${flight.landingsDay}D  ${flight.landingsNight}N", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (flight.remarks.isNotBlank()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                "Remarks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(flight.remarks, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeStatCol(label: String, minutes: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            minutes.toHhMm(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (minutes > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
