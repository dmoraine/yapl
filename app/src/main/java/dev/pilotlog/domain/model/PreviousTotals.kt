// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.model

import kotlinx.datetime.LocalDate

data class PreviousTotals(
    val asOf: LocalDate,
    val totalMinutes: Int = 0,
    val nightMinutes: Int = 0,
    val ifrMinutes: Int = 0,
    val picMinutes: Int = 0,
    val copilotMinutes: Int = 0,
    val dualMinutes: Int = 0,
    val instructorMinutes: Int = 0,
    // Previous landings are split day/night so the BCAA PDF can seed each column
    // correctly. They may be negative — used as a reconciliation offset when the
    // imported logbook's day/night split doesn't match the official paper totals.
    val totalLandingsDay: Int = 0,
    val totalLandingsNight: Int = 0,
    val totalTakeoffs: Int = 0,
)

fun FlightStats.withPreviousTotals(prev: PreviousTotals) = copy(
    totalMinutes      = totalMinutes      + prev.totalMinutes,
    nightMinutes      = nightMinutes      + prev.nightMinutes,
    ifrMinutes        = ifrMinutes        + prev.ifrMinutes,
    picMinutes        = picMinutes        + prev.picMinutes,
    copilotMinutes    = copilotMinutes    + prev.copilotMinutes,
    dualMinutes       = dualMinutes       + prev.dualMinutes,
    instructorMinutes = instructorMinutes + prev.instructorMinutes,
    totalLandings     = totalLandings     + prev.totalLandingsDay + prev.totalLandingsNight,
    totalTakeoffs     = totalTakeoffs     + prev.totalTakeoffs,
)
