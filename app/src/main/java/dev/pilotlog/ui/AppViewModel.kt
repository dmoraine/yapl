// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.DateFormat
import dev.pilotlog.domain.usecase.settings.ObserveSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-root state: cross-cutting preferences shared by every screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    val dateFormat: StateFlow<DateFormat> = observeSettings()
        .map { it.dateFormat }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DateFormat.DMY)
}
