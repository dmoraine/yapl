// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.repository

import dev.pilotlog.data.database.dao.FlightDao
import dev.pilotlog.data.mapper.toDomain
import dev.pilotlog.data.mapper.toEntity
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.model.FlightStats
import dev.pilotlog.domain.repository.FlightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class FlightRepositoryImpl @Inject constructor(
    private val dao: FlightDao,
) : FlightRepository {

    override fun getFlights(): Flow<List<Flight>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getFlightsByDateRange(from: LocalDate, to: LocalDate): Flow<List<Flight>> =
        dao.getByDateRange(from.toString(), to.toString())
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getFlightById(id: Long): Flight? =
        dao.getById(id)?.toDomain()

    override suspend fun getMostRecentFlight(): Flight? =
        dao.getMostRecent()?.toDomain()

    override suspend fun insertFlight(flight: Flight): Long =
        dao.insert(flight.toEntity())

    override suspend fun updateFlight(flight: Flight) =
        dao.update(flight.toEntity())

    override suspend fun deleteFlight(id: Long) =
        dao.delete(id)

    override suspend fun deleteAllFlights() =
        dao.deleteAll()

    override suspend fun getStats(from: LocalDate?, to: LocalDate?): FlightStats {
        val row = dao.getStats(from?.toString(), to?.toString())
        return FlightStats(
            totalMinutes      = row.totalMinutes,
            nightMinutes      = row.nightMinutes,
            ifrMinutes        = row.ifrMinutes,
            picMinutes        = row.picMinutes,
            copilotMinutes    = row.copilotMinutes,
            dualMinutes       = row.dualMinutes,
            instructorMinutes = row.instructorMinutes,
            totalFlights      = row.totalFlights,
            totalLandings     = row.totalLandings,
            totalTakeoffs     = row.totalTakeoffs,
        )
    }

    override suspend fun insertAll(flights: List<Flight>) =
        dao.insertAll(flights.map { it.toEntity() })
}
