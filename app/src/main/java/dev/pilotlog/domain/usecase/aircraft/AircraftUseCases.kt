// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.aircraft

import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.AircraftTypeWithCount
import dev.pilotlog.domain.repository.AircraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ── Types ──────────────────────────────────────────────────────────────────────

class GetAircraftTypesUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    operator fun invoke(): Flow<List<AircraftTypeWithCount>> = repository.getTypesWithCounts()
}

class GetAircraftTypeUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    suspend operator fun invoke(code: String): AircraftType? =
        repository.getType(code.trim().uppercase())
}

class SaveAircraftTypeUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    suspend operator fun invoke(type: AircraftType) = repository.saveType(
        type.copy(typeCode = type.typeCode.trim().uppercase()),
    )
}

class DeleteAircraftTypeUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    suspend operator fun invoke(code: String) = repository.deleteType(code)
}

// ── Registrations ───────────────────────────────────────────────────────────────

class GetRegistrationsByTypeUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    operator fun invoke(typeCode: String): Flow<List<Aircraft>> =
        repository.getRegistrationsByType(typeCode)
}

class SaveRegistrationUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    suspend operator fun invoke(aircraft: Aircraft) = repository.saveRegistration(
        aircraft.copy(registration = aircraft.registration.trim().uppercase()),
    )
}

class DeleteRegistrationUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    suspend operator fun invoke(registration: String) =
        repository.deleteRegistration(registration)
}

class GetAircraftByRegistrationUseCase @Inject constructor(
    private val repository: AircraftRepository,
) {
    suspend operator fun invoke(registration: String): Aircraft? =
        repository.getByRegistration(registration.trim().uppercase())
}
