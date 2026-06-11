// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.model.FlightStats
import dev.pilotlog.domain.usecase.flight.GetFlightsUseCase
import dev.pilotlog.domain.usecase.statistics.GetStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import javax.inject.Inject

data class HomeUiState(
    val recentFlights: List<Flight> = emptyList(),
    val allTimeStats: FlightStats? = null,
    val last90Stats: FlightStats? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFlights: GetFlightsUseCase,
    private val getStats: GetStatsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getFlights().collectLatest { flights ->
                val recent = flights.take(5)
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val from90 = today.minus(90, DateTimeUnit.DAY)
                val allStats = getStats()
                val recentStats = getStats(from = from90)
                _state.update {
                    it.copy(
                        recentFlights = recent,
                        allTimeStats = allStats,
                        last90Stats = recentStats,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
