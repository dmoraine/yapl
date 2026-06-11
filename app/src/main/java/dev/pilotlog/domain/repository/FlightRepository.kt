// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.repository

import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.model.FlightStats
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface FlightRepository {
    fun getFlights(): Flow<List<Flight>>
    fun getFlightsByDateRange(from: LocalDate, to: LocalDate): Flow<List<Flight>>
    suspend fun getFlightById(id: Long): Flight?
    suspend fun getMostRecentFlight(): Flight?
    suspend fun insertFlight(flight: Flight): Long
    suspend fun updateFlight(flight: Flight)
    suspend fun deleteFlight(id: Long)
    suspend fun deleteAllFlights()
    suspend fun getStats(from: LocalDate? = null, to: LocalDate? = null): FlightStats
    suspend fun insertAll(flights: List<Flight>)
}
