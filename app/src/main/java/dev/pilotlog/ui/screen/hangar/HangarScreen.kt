// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.hangar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.AircraftTypeWithCount
import dev.pilotlog.domain.model.EngineType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangarScreen(
    onTypeClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HangarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aircraft types") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openAddForm) {
                Icon(Icons.Filled.Add, contentDescription = "Add type")
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        if (state.types.isEmpty() && !state.isLoading) {
            EmptyHangar(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.types, key = { it.type.typeCode }) { item ->
                    SwipeableTypeRow(
                        item = item,
                        onClick = { onTypeClick(item.type.typeCode) },
                        onEdit = { viewModel.openEditForm(item.type) },
                        onDelete = { viewModel.delete(item.type) },
                    )
                }
            }
        }
    }

    state.editForm?.let { form ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissForm,
            sheetState = sheetState,
        ) {
            TypeForm(
                form = form,
                onTypeCodeChange = viewModel::onTypeCodeChange,
                onTypeNameChange = viewModel::onTypeNameChange,
                onEngineTypeChange = viewModel::onEngineTypeChange,
                onSave = viewModel::saveForm,
                onDismiss = viewModel::dismissForm,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTypeRow(
    item: AircraftTypeWithCount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
                false
            } else false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        TypeRow(item = item, onClick = onClick)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove ${item.type.typeCode}?") },
            text = {
                Text(
                    if (item.registrationCount > 0)
                        "This type has ${item.registrationCount} registration(s). They will remain but lose their type."
                    else "No registrations attached.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TypeRow(item: AircraftTypeWithCount, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.type.typeName.ifBlank { item.type.typeCode },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${item.type.typeCode} · ${if (item.type.engineType == EngineType.MULTI) "Multi" else "Single"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${item.registrationCount} reg.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypeForm(
    form: TypeFormState,
    onTypeCodeChange: (String) -> Unit,
    onTypeNameChange: (String) -> Unit,
    onEngineTypeChange: (EngineType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (form.originalCode == null) "Add aircraft type" else "Edit aircraft type",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = form.typeCode,
            onValueChange = onTypeCodeChange,
            label = { Text("ICAO type code (e.g. B738)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.typeName,
            onValueChange = onTypeNameChange,
            label = { Text("Type name (e.g. B737-800)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Engine:", style = MaterialTheme.typography.bodyMedium)
            FilterChip(
                selected = form.engineType == EngineType.SINGLE,
                onClick = { onEngineTypeChange(EngineType.SINGLE) },
                label = { Text("Single") },
            )
            FilterChip(
                selected = form.engineType == EngineType.MULTI,
                onClick = { onEngineTypeChange(EngineType.MULTI) },
                label = { Text("Multi") },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            TextButton(onClick = onSave, enabled = form.typeCode.isNotBlank()) { Text("Save") }
        }
    }
}

@Composable
private fun EmptyHangar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "No aircraft types",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Tap + to add your first type",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
