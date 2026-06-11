// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "previous_totals")
data class PreviousTotalsEntity(
    @PrimaryKey
    val id: Int = 1,               // Single-row table — always id=1
    @ColumnInfo(name = "as_of")           val asOf: String,
    @ColumnInfo(name = "total_min")       val totalMin: Int = 0,
    @ColumnInfo(name = "night_min")       val nightMin: Int = 0,
    @ColumnInfo(name = "ifr_min")         val ifrMin: Int = 0,
    @ColumnInfo(name = "pic_min")         val picMin: Int = 0,
    @ColumnInfo(name = "copilot_min")     val copilotMin: Int = 0,
    @ColumnInfo(name = "dual_min")        val dualMin: Int = 0,
    @ColumnInfo(name = "instructor_min")  val instructorMin: Int = 0,
    @ColumnInfo(name = "total_landings")        val totalLandingsDay: Int = 0,
    @ColumnInfo(name = "total_landings_night")  val totalLandingsNight: Int = 0,
    @ColumnInfo(name = "total_takeoffs")        val totalTakeoffs: Int = 0,
)
