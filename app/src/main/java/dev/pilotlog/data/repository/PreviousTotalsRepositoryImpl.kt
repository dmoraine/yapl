// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.repository

import dev.pilotlog.data.database.dao.PreviousTotalsDao
import dev.pilotlog.data.database.entity.PreviousTotalsEntity
import dev.pilotlog.domain.model.PreviousTotals
import dev.pilotlog.domain.repository.PreviousTotalsRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class PreviousTotalsRepositoryImpl @Inject constructor(
    private val dao: PreviousTotalsDao,
) : PreviousTotalsRepository {

    override suspend fun get(): PreviousTotals? =
        dao.get()?.toDomain()

    override suspend fun save(totals: PreviousTotals) =
        dao.upsert(totals.toEntity())

    override suspend fun clear() =
        dao.clear()

    private fun PreviousTotalsEntity.toDomain() = PreviousTotals(
        asOf              = LocalDate.parse(asOf),
        totalMinutes      = totalMin,
        nightMinutes      = nightMin,
        ifrMinutes        = ifrMin,
        picMinutes        = picMin,
        copilotMinutes    = copilotMin,
        dualMinutes       = dualMin,
        instructorMinutes = instructorMin,
        totalLandingsDay   = totalLandingsDay,
        totalLandingsNight = totalLandingsNight,
        totalTakeoffs      = totalTakeoffs,
    )

    private fun PreviousTotals.toEntity() = PreviousTotalsEntity(
        id             = 1,
        asOf           = asOf.toString(),
        totalMin       = totalMinutes,
        nightMin       = nightMinutes,
        ifrMin         = ifrMinutes,
        picMin         = picMinutes,
        copilotMin     = copilotMinutes,
        dualMin        = dualMinutes,
        instructorMin  = instructorMinutes,
        totalLandingsDay   = totalLandingsDay,
        totalLandingsNight = totalLandingsNight,
        totalTakeoffs      = totalTakeoffs,
    )
}
