// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.statistics

import dev.pilotlog.domain.model.FlightStats
import dev.pilotlog.domain.model.withPreviousTotals
import dev.pilotlog.domain.repository.FlightRepository
import dev.pilotlog.domain.repository.PreviousTotalsRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class GetStatsUseCase @Inject constructor(
    private val repository: FlightRepository,
    private val previousTotalsRepository: PreviousTotalsRepository,
) {
    suspend operator fun invoke(from: LocalDate? = null, to: LocalDate? = null): FlightStats {
        val stats = repository.getStats(from, to)
        // Only add carried-forward totals to all-time view (no date filter)
        if (from == null && to == null) {
            val prev = previousTotalsRepository.get()
            if (prev != null) return stats.withPreviousTotals(prev)
        }
        return stats
    }
}
