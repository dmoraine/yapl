// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.mapper

import dev.pilotlog.data.database.entity.AircraftEntity
import dev.pilotlog.data.database.entity.AircraftTypeEntity
import dev.pilotlog.data.database.entity.TypeWithCount
import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.AircraftTypeWithCount
import dev.pilotlog.domain.model.EngineType

fun AircraftTypeEntity.toDomain(): AircraftType = AircraftType(
    typeCode   = typeCode,
    typeName   = typeName,
    engineType = EngineType.valueOf(engineType),
)

fun AircraftType.toEntity(): AircraftTypeEntity = AircraftTypeEntity(
    typeCode   = typeCode,
    typeName   = typeName,
    engineType = engineType.name,
)

fun AircraftEntity.toDomain(): Aircraft = Aircraft(
    registration = registration,
    typeCode     = typeCode,
)

fun Aircraft.toEntity(): AircraftEntity = AircraftEntity(
    registration = registration,
    typeCode     = typeCode,
)

fun TypeWithCount.toDomain(): AircraftTypeWithCount = AircraftTypeWithCount(
    type = AircraftType(
        typeCode   = typeCode,
        typeName   = typeName,
        engineType = EngineType.valueOf(engineType),
    ),
    registrationCount = regCount,
)
