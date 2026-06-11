// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.model

/** Aggregated statistics for an arbitrary set of flights. */
data class FlightStats(
    val totalMinutes: Int,
    val nightMinutes: Int,
    val ifrMinutes: Int,
    val picMinutes: Int,
    val copilotMinutes: Int,
    val dualMinutes: Int,
    val instructorMinutes: Int,
    val totalFlights: Int,
    val totalLandings: Int,
    val totalTakeoffs: Int,
)
