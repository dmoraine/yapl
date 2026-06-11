// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.model

/** An aircraft type / variant, e.g. B738 "Boeing 737-800". */
data class AircraftType(
    val typeCode: String,        // ICAO type designator e.g. "B738" — primary key
    val typeName: String,        // Human-readable e.g. "B737-800"
    val engineType: EngineType,
)

/** A single registered airframe, e.g. "OE-IWA", belonging to one [AircraftType]. */
data class Aircraft(
    val registration: String,    // primary key
    val typeCode: String,        // FK → AircraftType.typeCode
)

/** An aircraft type with the number of registrations attached (for list display). */
data class AircraftTypeWithCount(
    val type: AircraftType,
    val registrationCount: Int,
)

enum class EngineType { SINGLE, MULTI }
