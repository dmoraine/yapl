// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.mapper

import dev.pilotlog.data.database.entity.AirportEntity
import dev.pilotlog.domain.model.Airport

fun AirportEntity.toDomain(): Airport = Airport(
    icao         = icao,
    iata         = iata,
    name         = name,
    municipality = municipality,
    country      = country,
    latitude     = latitude,
    longitude    = longitude,
    elevationFt  = elevationFt,
    timezone     = timezone,
    isCustom     = isCustom,
)

fun Airport.toEntity(): AirportEntity = AirportEntity(
    icao         = icao,
    iata         = iata,
    name         = name,
    municipality = municipality,
    country      = country,
    latitude     = latitude,
    longitude    = longitude,
    elevationFt  = elevationFt,
    timezone     = timezone,
    isCustom     = isCustom,
)
