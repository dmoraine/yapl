// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.airport

import dev.pilotlog.domain.model.Airport
import dev.pilotlog.domain.repository.AirportRepository
import javax.inject.Inject

class GetAirportByIcaoUseCase @Inject constructor(
    private val repository: AirportRepository,
) {
    suspend operator fun invoke(icao: String): Airport? =
        repository.getByIcao(icao.trim().uppercase())
}
