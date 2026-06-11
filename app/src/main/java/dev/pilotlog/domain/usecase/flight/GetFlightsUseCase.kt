// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.flight

import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.repository.FlightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class GetFlightsUseCase @Inject constructor(
    private val repository: FlightRepository,
) {
    operator fun invoke(): Flow<List<Flight>> = repository.getFlights()

    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<Flight>> =
        repository.getFlightsByDateRange(from, to)
}
