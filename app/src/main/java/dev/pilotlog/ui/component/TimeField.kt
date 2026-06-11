// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayText = value?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("HH:MM") },
            trailingIcon = {
                Icon(Icons.Filled.Schedule, contentDescription = null)
            },
            readOnly = true,
            singleLine = true,
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay — captures tap without fighting with TextField focus
        Box(Modifier.matchParentSize().clickable { showDialog = true })
    }

    if (showDialog) {
        TimePickerDialog(
            initialTime = value,
            onDismiss = { showDialog = false },
            onConfirm = { picked ->
                onValueChange(picked)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var showKeyboard by remember { mutableStateOf(false) }
    val pickerState = rememberTimePickerState(
        initialHour   = initialTime?.hour ?: 0,
        initialMinute = initialTime?.minute ?: 0,
        is24Hour      = true,
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape        = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text  = "UTC",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                )

                if (showKeyboard) {
                    TimeInput(state = pickerState)
                } else {
                    TimePicker(state = pickerState)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showKeyboard = !showKeyboard }) {
                        Icon(
                            imageVector = if (showKeyboard) Icons.Filled.Schedule else Icons.Filled.Keyboard,
                            contentDescription = if (showKeyboard) "Switch to clock" else "Switch to keyboard",
                        )
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        TextButton(onClick = {
                            onConfirm(LocalTime(pickerState.hour, pickerState.minute))
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

// Kept for callers that parse raw "HHMM" strings (e.g. legacy import)
fun parseTimeInput(input: String): LocalTime? {
    val digits = input.filter { it.isDigit() }
    return when (digits.length) {
        4 -> {
            val h = digits.substring(0, 2).toIntOrNull() ?: return null
            val m = digits.substring(2, 4).toIntOrNull() ?: return null
            if (h in 0..23 && m in 0..59) LocalTime(h, m) else null
        }
        else -> null
    }
}
