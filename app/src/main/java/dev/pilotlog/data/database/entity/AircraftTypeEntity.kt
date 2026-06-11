// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aircraft_types")
data class AircraftTypeEntity(
    @PrimaryKey
    @ColumnInfo(name = "type_code")   val typeCode: String,
    @ColumnInfo(name = "type_name")   val typeName: String,
    @ColumnInfo(name = "engine_type") val engineType: String,   // "SINGLE" | "MULTI"
)
