// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.model

data class Airport(
    val icao: String,
    val iata: String,
    val name: String,
    val municipality: String,
    val country: String,
    /** Null for airports imported without coordinate data; required for night-time calculation. */
    val latitude: Double?,
    val longitude: Double?,
    val elevationFt: Int?,
    val timezone: String,
    /** True for airports added or modified by the user (never overwritten by DB updates). */
    val isCustom: Boolean = false,
)
