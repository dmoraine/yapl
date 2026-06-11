// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.flight

import dev.pilotlog.domain.repository.FlightRepository
import javax.inject.Inject

class DeleteFlightUseCase @Inject constructor(
    private val repository: FlightRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteFlight(id)
}
