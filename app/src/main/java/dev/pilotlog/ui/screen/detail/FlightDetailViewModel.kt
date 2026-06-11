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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _state = MutableStateFlow(FlightDetailUiState())
    val state: StateFlow<FlightDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val flight = flightRepository.getFlightById(flightId)
            _state.update { it.copy(flight = flight, isLoading = false) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            deleteFlight(flightId)
            _state.update { it.copy(deleted = true) }
        }
    }
}
