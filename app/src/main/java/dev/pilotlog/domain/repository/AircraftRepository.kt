// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.repository

import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.AircraftTypeWithCount
import kotlinx.coroutines.flow.Flow

interface AircraftRepository {

    // Types
    fun getTypes(): Flow<List<AircraftType>>
    fun getTypesWithCounts(): Flow<List<AircraftTypeWithCount>>
    suspend fun getType(code: String): AircraftType?
    suspend fun saveType(type: AircraftType)
    suspend fun deleteType(code: String)

    // Registrations
    fun getRegistrationsByType(typeCode: String): Flow<List<Aircraft>>
    fun getAllRegistrations(): Flow<List<Aircraft>>
    suspend fun getByRegistration(registration: String): Aircraft?
    suspend fun saveRegistration(aircraft: Aircraft)
    suspend fun deleteRegistration(registration: String)
}
