// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "airports",
    indices = [
        Index(value = ["icao"]),
        Index(value = ["iata"]),
        Index(value = ["name"]),
        Index(value = ["municipality"]),
    ],
)
data class AirportEntity(
    @PrimaryKey
    val icao: String,
    @ColumnInfo(name = "iata")          val iata: String,
    @ColumnInfo(name = "name")          val name: String,
    @ColumnInfo(name = "municipality")  val municipality: String,
    @ColumnInfo(name = "country")       val country: String,
    /** Nullable — populated for airports enriched with OurAirports coordinates. */
    @ColumnInfo(name = "latitude")      val latitude: Double?,
    @ColumnInfo(name = "longitude")     val longitude: Double?,
    @ColumnInfo(name = "elevation_ft")  val elevationFt: Int?,
    @ColumnInfo(name = "timezone")      val timezone: String,
    /** 1 = added or edited by user; never overwritten during DB migrations. */
    @ColumnInfo(name = "is_custom")     val isCustom: Boolean = false,
)
