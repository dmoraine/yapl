// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Core domain model for a logbook flight entry.
 *
 * All times are in UTC. Durations are stored in whole minutes.
 * Night time is computed automatically from departure/arrival positions and times;
 * it can be manually overridden if needed (e.g. for imported legacy data).
 */
data class Flight(
    val id: Long = 0,

    // Route
    val date: LocalDate,
    val departureAirport: String,   // ICAO
    val departureTime: LocalTime,
    val arrivalAirport: String,     // ICAO
    val arrivalTime: LocalTime,

    // Aircraft
    val aircraftType: String,       // ICAO type code
    val aircraftRegistration: String,

    // Computed durations (minutes)
    val totalMinutes: Int,
    val nightMinutes: Int,
    val ifrMinutes: Int,
    val picMinutes: Int,
    val copilotMinutes: Int,
    val dualMinutes: Int,
    val instructorMinutes: Int,

    // Operations
    val takeoffsDay: Int,
    val takeoffsNight: Int,
    val landingsDay: Int,
    val landingsNight: Int,
    val isMultiPilot: Boolean,

    // Crew
    val picName: String = "",

    // Commercial
    val flightNumber: String = "",
    val remarks: String = "",

    // Logbook pagination — true if this flight is the last row of a paper-logbook page.
    val pageBreak: Boolean = false,
)
