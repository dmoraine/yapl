// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.airport

import dev.pilotlog.domain.model.Airport
import dev.pilotlog.domain.repository.AirportRepository
import javax.inject.Inject

class SearchAirportsUseCase @Inject constructor(
    private val repository: AirportRepository,
) {
    suspend operator fun invoke(query: String): List<Airport> =
        repository.search(query.trim().uppercase())
}
