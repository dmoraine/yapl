// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.entity

import androidx.room.ColumnInfo

/** Projection row for [dev.pilotlog.data.database.dao.AircraftDao.getTypesWithCounts]. */
data class TypeWithCount(
    @ColumnInfo(name = "type_code")   val typeCode: String,
    @ColumnInfo(name = "type_name")   val typeName: String,
    @ColumnInfo(name = "engine_type") val engineType: String,
    @ColumnInfo(name = "reg_count")   val regCount: Int,
)
