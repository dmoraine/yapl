// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.usecase.flight.DeleteFlightUseCase
import dev.pilotlog.domain.repository.FlightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlightDetailUiState(
    val flight: Flight? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false,
)

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flightRepository: FlightRepository,
    private val deleteFlight: DeleteFlightUseCase,
) : ViewModel() {

    private val flightId: Long = checkNotNull(savedStateHandle["flightId"])

    // Local one-shot signal: the reactive flight Flow emits null once the row is
    // deleted, which is indistinguishable from "not found". Keep deletion separate
    // so the screen can navigate away instead of rendering an empty detail.
    private val deletedSignal = MutableStateFlow(false)

    // Observe the flight reactively so edits made on the Add/Edit screen are
    // reflected here as soon as Room emits, without recreating this ViewModel.
    val state: StateFlow<FlightDetailUiState> =
        combine(flightRepository.getFlightByIdFlow(flightId), deletedSignal) { flight, deleted ->
            FlightDetailUiState(flight = flight, isLoading = false, deleted = deleted)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FlightDetailUiState(isLoading = true),
        )

    fun delete() {
        viewModelScope.launch {
            deleteFlight(flightId)
            deletedSignal.value = true
        }
    }
}
