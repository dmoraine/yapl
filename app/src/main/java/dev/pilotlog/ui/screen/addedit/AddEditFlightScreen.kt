// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.EngineType
import dev.pilotlog.ui.component.AddAirportDialog
import dev.pilotlog.ui.component.AirportSearchField
import dev.pilotlog.ui.component.DurationPickerField
import dev.pilotlog.ui.component.TimeField
import dev.pilotlog.ui.util.LocalDateFormat
import dev.pilotlog.ui.util.formatted
import dev.pilotlog.ui.util.toHhMm
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFlightScreen(
    onNavigateUp: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddEditFlightViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    // Inline "add" dialogs triggered from the route / aircraft fields.
    var addAirportTarget by remember { mutableStateOf<AirportTarget?>(null) }
    var showAddType by remember { mutableStateOf(false) }
    var showAddRegistration by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedFlightId) {
        if (state.savedFlightId != null) onSaved(state.savedFlightId!!)
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            snackbarHost.showSnackbar(state.error!!)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.flightId == null) "New Flight" else "Edit Flight")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.save() }) {
                            Icon(Icons.Filled.Check, contentDescription = "Save")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            SectionLabel("Route")
            DateRow(date = state.date, onDateChange = viewModel::onDateChange)
            Spacer(Modifier.height(12.dp))
            AirportSearchField(
                query = state.depQuery,
                onQueryChange = viewModel::onDepQueryChange,
                suggestions = state.depSuggestions,
                onAirportSelected = viewModel::onDepAirportSelected,
                label = "Departure",
                hasSelection = state.depAirport != null,
                onAddAirport = { addAirportTarget = AirportTarget.DEPARTURE },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField(
                    value = state.depTime,
                    onValueChange = viewModel::onDepTimeChange,
                    label = "OFF (UTC)",
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    value = state.arrTime,
                    onValueChange = viewModel::onArrTimeChange,
                    label = "ON (UTC)",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            AirportSearchField(
                query = state.arrQuery,
                onQueryChange = viewModel::onArrQueryChange,
                suggestions = state.arrSuggestions,
                onAirportSelected = viewModel::onArrAirportSelected,
                label = "Arrival",
                hasSelection = state.arrAirport != null,
                onAddAirport = { addAirportTarget = AirportTarget.ARRIVAL },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            DurationRow(
                label = "Total time",
                minutes = state.totalMinutes,
                onMinutesChange = viewModel::onTotalMinutesChange,
            )

            SectionDivider()
            SectionLabel("Times")

            NightTimeRow(state = state, viewModel = viewModel)
            Spacer(Modifier.height(12.dp))
            DurationPickerField(
                label = "IFR time",
                minutes = state.ifrMinutes,
                onMinutesChange = viewModel::onIfrMinutesChange,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionDivider()
            SectionLabel("Aircraft")

            DropdownField(
                label = "Type",
                value = state.aircraftType,
                options = state.availableTypes.map { it.typeCode to it.typeName.ifBlank { it.typeCode } },
                onSelected = viewModel::onTypeSelected,
                onAdd = { showAddType = true },
                addLabel = "Add a type",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = if (state.aircraftType.isBlank()) "Registration (pick a type first)" else "Registration",
                value = state.registration,
                options = state.availableRegistrations.map { it to it },
                onSelected = viewModel::onRegistrationSelected,
                enabled = state.aircraftType.isNotBlank(),
                onAdd = if (state.aircraftType.isNotBlank()) ({ showAddRegistration = true }) else null,
                addLabel = "Add a registration",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Multi-crew", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.isMultiPilot,
                    onCheckedChange = viewModel::onMultiPilotChange,
                )
            }

            SectionDivider()
            SectionLabel("Role")

            RoleShortcutRow(state = state, viewModel = viewModel)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DurationRow(
                    label = "PIC",
                    minutes = state.picMinutes,
                    onMinutesChange = viewModel::onPicMinutesChange,
                    modifier = Modifier.weight(1f),
                )
                DurationRow(
                    label = "Copilot",
                    minutes = state.copilotMinutes,
                    onMinutesChange = viewModel::onCopilotMinutesChange,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DurationRow(
                    label = "Dual",
                    minutes = state.dualMinutes,
                    onMinutesChange = viewModel::onDualMinutesChange,
                    modifier = Modifier.weight(1f),
                )
                DurationRow(
                    label = "Instructor",
                    minutes = state.instructorMinutes,
                    onMinutesChange = viewModel::onInstructorMinutesChange,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionDivider()
            SectionLabel("Crew")

            OutlinedTextField(
                value = state.picName,
                onValueChange = viewModel::onPicNameChange,
                label = { Text("Name of PIC") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionDivider()
            SectionLabel("Operations")

            OperationToggle(
                label = "Take-off by me",
                byMe = state.takeoffByMe,
                isNight = state.depIsNight,
                onChange = viewModel::onTakeoffByMeChange,
            )
            Spacer(Modifier.height(4.dp))
            OperationToggle(
                label = "Landing by me",
                byMe = state.landingByMe,
                isNight = state.arrIsNight,
                onChange = viewModel::onLandingByMeChange,
            )

            SectionDivider()
            SectionLabel("Notes")

            OutlinedTextField(
                value = state.flightNumber,
                onValueChange = viewModel::onFlightNumberChange,
                label = { Text("Flight number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.remarks,
                onValueChange = viewModel::onRemarksChange,
                label = { Text("Remarks") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Inline "add" dialogs ────────────────────────────────────────────────────
    addAirportTarget?.let { target ->
        val prefill = if (target == AirportTarget.DEPARTURE) state.depQuery else state.arrQuery
        AddAirportDialog(
            initialIcao = prefill,
            onDismiss = { addAirportTarget = null },
            onConfirm = { airport ->
                viewModel.addCustomAirport(airport, isDeparture = target == AirportTarget.DEPARTURE)
                addAirportTarget = null
            },
        )
    }

    if (showAddType) {
        AddAircraftTypeDialog(
            onDismiss = { showAddType = false },
            onConfirm = { type ->
                viewModel.addAircraftType(type)
                showAddType = false
            },
        )
    }

    if (showAddRegistration) {
        AddRegistrationDialog(
            typeCode = state.aircraftType,
            onDismiss = { showAddRegistration = false },
            onConfirm = { reg ->
                viewModel.addRegistration(reg)
                showAddRegistration = false
            },
        )
    }
}

private enum class AirportTarget { DEPARTURE, ARRIVAL }

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDateTime(date, LocalTime(0, 0))
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds(),
    )

    OutlinedTextField(
        value = date.formatted(LocalDateFormat.current),
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val ld = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onDateChange(ld)
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun NightTimeRow(state: FlightFormState, viewModel: AddEditFlightViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.NightsStay,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        DurationRow(
            label = "Night time",
            minutes = state.nightMinutes,
            onMinutesChange = viewModel::onNightMinutesChange,
            modifier = Modifier.weight(1f),
        )
        if (state.isNightCalculating) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else if (!state.nightIsAuto) {
            IconButton(
                onClick = viewModel::onNightAutoReset,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Recalculate",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (!state.nightIsAuto) {
        Text(
            "Manual override — tap ↺ to recalculate",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,   // value to display label
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onAdd: (() -> Unit)? = null,
    addLabel: String = "Add new",
) {
    var expanded by remember { mutableStateOf(false) }
    val hasMenu = options.isNotEmpty() || onAdd != null

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        if (hasMenu) {
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (optValue, optLabel) ->
                    DropdownMenuItem(
                        text = {
                            if (optValue == optLabel) {
                                Text(optValue, fontWeight = FontWeight.SemiBold)
                            } else {
                                Column {
                                    Text(optLabel, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        optValue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelected(optValue)
                            expanded = false
                        },
                    )
                }
                if (onAdd != null) {
                    if (options.isNotEmpty()) HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    addLabel,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        onClick = {
                            onAdd()
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleShortcutRow(state: FlightFormState, viewModel: AddEditFlightViewModel) {
    val total = state.totalMinutes
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.picMinutes == total && total > 0,
            onClick = viewModel::applyRoleCaptain,
            label = { Text("CPT") },
        )
        FilterChip(
            selected = state.copilotMinutes == total && total > 0,
            onClick = viewModel::applyRoleFO,
            label = { Text("F/O") },
        )
        FilterChip(
            selected = state.dualMinutes == total && total > 0,
            onClick = viewModel::applyRoleStudent,
            label = { Text("Dual") },
        )
        FilterChip(
            selected = state.instructorMinutes == total && total > 0,
            onClick = viewModel::applyRoleInstructor,
            label = { Text("Instr") },
        )
    }
}

@Composable
private fun DurationRow(
    label: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(minutes) { mutableStateOf(minutes.toHhMm()) }

    LaunchedEffect(minutes) {
        val formatted = minutes.toHhMm()
        if (dev.pilotlog.ui.util.parseHhMm(text) != minutes) text = formatted
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || it == ':' }.take(5)
            text = filtered
            val parsed = dev.pilotlog.ui.util.parseHhMm(filtered)
            if (parsed != null) onMinutesChange(parsed)
        },
        label = { Text(label) },
        placeholder = { Text("H:MM") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun OperationToggle(
    label: String,
    byMe: Boolean,
    isNight: Boolean?,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (byMe) {
            val night = isNight == true
            Icon(
                imageVector = if (night) Icons.Filled.NightsStay else Icons.Filled.WbSunny,
                contentDescription = null,
                tint = if (night) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (night) "Night" else "Day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
        }
        Switch(checked = byMe, onCheckedChange = onChange)
    }
}

@Composable
private fun AddAircraftTypeDialog(
    onDismiss: () -> Unit,
    onConfirm: (AircraftType) -> Unit,
) {
    var typeCode by remember { mutableStateOf("") }
    var typeName by remember { mutableStateOf("") }
    var engineType by remember { mutableStateOf(EngineType.MULTI) }
    val canSave = typeCode.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add aircraft type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = typeCode,
                    onValueChange = { typeCode = it.uppercase().take(8) },
                    label = { Text("Type code * (e.g. B738)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = typeName,
                    onValueChange = { typeName = it },
                    label = { Text("Name (e.g. Boeing 737-800)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = engineType == EngineType.SINGLE,
                        onClick = { engineType = EngineType.SINGLE },
                        label = { Text("Single") },
                    )
                    FilterChip(
                        selected = engineType == EngineType.MULTI,
                        onClick = { engineType = EngineType.MULTI },
                        label = { Text("Multi") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onConfirm(
                        AircraftType(
                            typeCode = typeCode.trim().uppercase(),
                            typeName = typeName.trim(),
                            engineType = engineType,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddRegistrationDialog(
    typeCode: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var registration by remember { mutableStateOf("") }
    val canSave = registration.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add registration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Type $typeCode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = registration,
                    onValueChange = { registration = it.uppercase() },
                    label = { Text("Registration (e.g. OO-ABC)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(registration.trim().uppercase()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
