// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.repository

import dev.pilotlog.data.database.dao.AircraftDao
import dev.pilotlog.data.mapper.toDomain
import dev.pilotlog.data.mapper.toEntity
import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.AircraftTypeWithCount
import dev.pilotlog.domain.repository.AircraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AircraftRepositoryImpl @Inject constructor(
    private val dao: AircraftDao,
) : AircraftRepository {

    // ── Types ──────────────────────────────────────────────────────────────────

    override fun getTypes(): Flow<List<AircraftType>> =
        dao.getTypes().map { list -> list.map { it.toDomain() } }

    override fun getTypesWithCounts(): Flow<List<AircraftTypeWithCount>> =
        dao.getTypesWithCounts().map { list -> list.map { it.toDomain() } }

    override suspend fun getType(code: String): AircraftType? =
        dao.getType(code)?.toDomain()

    override suspend fun saveType(type: AircraftType) =
        dao.upsertType(type.toEntity())

    override suspend fun deleteType(code: String) =
        dao.deleteType(code)

    // ── Registrations ──────────────────────────────────────────────────────────

    override fun getRegistrationsByType(typeCode: String): Flow<List<Aircraft>> =
        dao.getRegistrationsByType(typeCode).map { list -> list.map { it.toDomain() } }

    override fun getAllRegistrations(): Flow<List<Aircraft>> =
        dao.getAllRegistrations().map { list -> list.map { it.toDomain() } }

    override suspend fun getByRegistration(registration: String): Aircraft? =
        dao.getByRegistration(registration)?.toDomain()

    override suspend fun saveRegistration(aircraft: Aircraft) =
        dao.upsertRegistration(aircraft.toEntity())

    override suspend fun deleteRegistration(registration: String) =
        dao.deleteRegistration(registration)
}
