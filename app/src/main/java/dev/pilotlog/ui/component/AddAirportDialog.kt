// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.pilotlog.domain.model.Airport

/**
 * Minimal form to create a missing airport while logging a flight.
 * ICAO, name and coordinates are required — coordinates feed the automatic
 * night-time calculation; the rest is optional.
 */
@Composable
fun AddAirportDialog(
    initialIcao: String,
    onDismiss: () -> Unit,
    onConfirm: (Airport) -> Unit,
) {
    var icao by remember { mutableStateOf(initialIcao.trim().uppercase()) }
    var name by remember { mutableStateOf("") }
    var iata by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }

    val latValue = lat.trim().toDoubleOrNull()
    val lonValue = lon.trim().toDoubleOrNull()
    val icaoValid = icao.trim().length in 3..4 && icao.trim().all { it.isLetterOrDigit() }
    val latValid = latValue != null && latValue in -90.0..90.0
    val lonValid = lonValue != null && lonValue in -180.0..180.0
    val canSave = icaoValid && name.isNotBlank() && latValid && lonValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add airport") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = icao,
                    onValueChange = { icao = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(4) },
                    label = { Text("ICAO *") },
                    singleLine = true,
                    isError = icao.isNotBlank() && !icaoValid,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text("Latitude *") },
                        placeholder = { Text("50.6374") },
                        singleLine = true,
                        isError = lat.isNotBlank() && !latValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = lon,
                        onValueChange = { lon = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text("Longitude *") },
                        placeholder = { Text("5.4432") },
                        singleLine = true,
                        isError = lon.isNotBlank() && !lonValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = iata,
                        onValueChange = { iata = it.uppercase().filter { c -> c.isLetter() }.take(3) },
                        label = { Text("IATA") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Coordinates in decimal degrees. They power the automatic night-time calculation.",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onConfirm(
                        Airport(
                            icao = icao.trim().uppercase(),
                            iata = iata.trim().uppercase(),
                            name = name.trim(),
                            municipality = "",
                            country = country.trim(),
                            latitude = latValue,
                            longitude = lonValue,
                            elevationFt = null,
                            timezone = "UTC",
                            isCustom = true,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
