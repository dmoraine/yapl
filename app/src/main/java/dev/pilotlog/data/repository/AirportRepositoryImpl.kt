// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.repository

import dev.pilotlog.data.database.dao.AirportDao
import dev.pilotlog.data.mapper.toDomain
import dev.pilotlog.data.mapper.toEntity
import dev.pilotlog.domain.model.Airport
import dev.pilotlog.domain.repository.AirportRepository
import javax.inject.Inject

class AirportRepositoryImpl @Inject constructor(
    private val dao: AirportDao,
) : AirportRepository {

    override suspend fun search(query: String, limit: Int): List<Airport> =
        dao.search(query, limit).map { it.toDomain() }

    override suspend fun getCustomAirports(): List<Airport> =
        dao.getCustom().map { it.toDomain() }

    override suspend fun getByIcao(icao: String): Airport? =
        dao.getByIcao(icao)?.toDomain()

    override suspend fun getByIata(iata: String): Airport? =
        dao.getByIata(iata)?.toDomain()

    override suspend fun saveCustomAirport(airport: Airport) =
        dao.upsert(airport.copy(isCustom = true).toEntity())

    override suspend fun deleteCustomAirport(icao: String) =
        dao.deleteCustom(icao)
}
