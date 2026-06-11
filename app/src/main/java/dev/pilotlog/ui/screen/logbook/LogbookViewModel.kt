// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.logbook.LogbookPaging
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.usecase.flight.DeleteFlightUseCase
import dev.pilotlog.domain.usecase.flight.GetFlightsUseCase
import dev.pilotlog.domain.usecase.flight.UpdateFlightUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogbookUiState(
    val groups: List<MonthGroup> = emptyList(),
    val totalMinutes: Int = 0,
    val isLoading: Boolean = true,
    val flightsAsc: List<Flight> = emptyList(),   // oldest → newest, for page-break paging
)

data class MonthGroup(
    val label: String,         // e.g. "June 2026"
    val flights: List<Flight>,
    val totalMinutes: Int,
)

@HiltViewModel
class LogbookViewModel @Inject constructor(
    private val getFlights: GetFlightsUseCase,
    private val deleteFlight: DeleteFlightUseCase,
    private val updateFlight: UpdateFlightUseCase,
) : ViewModel() {

    val uiState: StateFlow<LogbookUiState> = getFlights()
        .map { flights ->
            val groups = flights
                .groupBy { it.date.let { d -> "%04d-%02d".format(d.year, d.monthNumber) } }
                .map { (key, groupFlights) ->
                    val date = groupFlights.first().date
                    MonthGroup(
                        label = "${monthName(date.monthNumber)} ${date.year}",
                        flights = groupFlights,
                        totalMinutes = groupFlights.sumOf { it.totalMinutes },
                    )
                }
            LogbookUiState(
                groups = groups,
                totalMinutes = flights.sumOf { it.totalMinutes },
                isLoading = false,
                flightsAsc = flights.sortedWith(compareBy({ it.date }, { it.departureTime })),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogbookUiState())

    fun delete(flight: Flight) {
        viewModelScope.launch { deleteFlight(flight.id) }
    }

    /** Mark / unmark a flight as the end of a paper-logbook page. */
    fun togglePageBreak(flight: Flight) {
        viewModelScope.launch { updateFlight(flight.copy(pageBreak = !flight.pageBreak)) }
    }

    /** Number of flights on the page ending at [flight] (for the long-press dialog). */
    fun pageSizeFor(flight: Flight): Int {
        val asc = uiState.value.flightsAsc
        val index = asc.indexOfFirst { it.id == flight.id }
        if (index < 0) return 0
        return LogbookPaging.pageSizeEndingAt(asc, index) { it.pageBreak }
    }

    private fun monthName(m: Int) = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )[m - 1]
}
