// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.maintenance

import dev.pilotlog.domain.repository.FlightRepository
import javax.inject.Inject

/** Delete every flight (e.g. before a clean re-import). Aircraft and airports are kept. */
class ClearAllFlightsUseCase @Inject constructor(
    private val flightRepository: FlightRepository,
) {
    suspend operator fun invoke() = flightRepository.deleteAllFlights()
}
