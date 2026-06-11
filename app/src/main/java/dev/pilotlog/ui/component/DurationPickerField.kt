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
import dev.pilotlog.ui.util.toHhMm

/** Tap-to-open H:MM duration picker, backed by the M3 time picker dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPickerField(
    label: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = if (minutes > 0) minutes.toHhMm() else "",
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("H:MM") },
            trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { showDialog = true })
    }

    if (showDialog) {
        DurationPickerDialog(
            initialMinutes = minutes,
            label = label,
            onDismiss = { showDialog = false },
            onConfirm = {
                onMinutesChange(it)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationPickerDialog(
    initialMinutes: Int,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var showKeyboard by remember { mutableStateOf(true) }   // durations: keyboard first
    val pickerState = rememberTimePickerState(
        initialHour   = (initialMinutes / 60).coerceIn(0, 23),
        initialMinute = initialMinutes % 60,
        is24Hour      = true,
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
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
                            onConfirm(pickerState.hour * 60 + pickerState.minute)
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
