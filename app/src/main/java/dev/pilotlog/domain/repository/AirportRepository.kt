// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.repository

import dev.pilotlog.domain.model.Airport

interface AirportRepository {
    suspend fun search(query: String, limit: Int = 10): List<Airport>
    /** All user-created airports (isCustom = true), for backup/export. */
    suspend fun getCustomAirports(): List<Airport>
    suspend fun getByIcao(icao: String): Airport?
    suspend fun getByIata(iata: String): Airport?
    /** Save a user-created or user-edited airport (always sets isCustom = true). */
    suspend fun saveCustomAirport(airport: Airport)
    /** Delete a user-added airport. No-op for bundled airports. */
    suspend fun deleteCustomAirport(icao: String)
}
