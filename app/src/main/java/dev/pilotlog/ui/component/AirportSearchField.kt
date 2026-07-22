// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.pilotlog.domain.model.Airport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirportSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<Airport>,
    onAirportSelected: (Airport) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    selected: Airport? = null,
    onAddAirport: (() -> Unit)? = null,
) {
    // Offer "add airport" only while actively searching an unmatched query.
    val showAddRow = onAddAirport != null && query.trim().length >= 2 && selected == null
    // A resolved code closes the dropdown silently, so name the match under the field —
    // otherwise "found it" and "cannot create it" look identical to the user.
    val resolved = selected?.let { "${it.name}, ${it.country}" }
    // Tied to showAddRow: the message points at that row, so never say it without one.
    val unmatched = showAddRow && suggestions.isEmpty()
    var expanded by remember { mutableStateOf(false) }
    expanded = suggestions.isNotEmpty() || showAddRow

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && (suggestions.isNotEmpty() || showAddRow) },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                // Codes are uppercase by convention and the search uppercases anyway,
                // so echo back what is actually being looked up.
                val typed = it.uppercase()
                onQueryChange(typed)
                if (typed.isBlank()) expanded = false
            },
            label = { Text(label) },
            placeholder = { Text("ICAO / IATA / name") },
            supportingText = when {
                resolved != null -> {
                    { Text(resolved, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                unmatched -> {
                    { Text("Unknown code - tap to add it", color = MaterialTheme.colorScheme.error) }
                }
                else -> null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
        )

        if (suggestions.isNotEmpty() || showAddRow) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                suggestions.forEach { airport ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "${airport.icao}  ${airport.iata.ifBlank { "---" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${airport.name}, ${airport.country}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onAirportSelected(airport)
                            expanded = false
                        },
                    )
                }
                if (showAddRow) {
                    if (suggestions.isNotEmpty()) HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.AddLocationAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Add airport \"${query.trim().uppercase()}\"",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        onClick = {
                            onAddAirport?.invoke()
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
