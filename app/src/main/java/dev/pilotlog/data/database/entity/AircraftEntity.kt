// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "aircraft",
    indices = [Index(value = ["type_code"])],
)
data class AircraftEntity(
    @PrimaryKey
    @ColumnInfo(name = "registration") val registration: String,
    @ColumnInfo(name = "type_code")    val typeCode: String,
)
