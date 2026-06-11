// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Route
    @ColumnInfo(name = "date")               val date: String,           // ISO-8601 yyyy-MM-dd
    @ColumnInfo(name = "dep_airport")        val depAirport: String,
    @ColumnInfo(name = "dep_time")           val depTime: String,        // HH:mm
    @ColumnInfo(name = "arr_airport")        val arrAirport: String,
    @ColumnInfo(name = "arr_time")           val arrTime: String,

    // Aircraft
    @ColumnInfo(name = "ac_type")            val acType: String,
    @ColumnInfo(name = "ac_registration")    val acRegistration: String,

    // Durations (whole minutes)
    @ColumnInfo(name = "total_min")          val totalMin: Int,
    @ColumnInfo(name = "night_min")          val nightMin: Int,
    @ColumnInfo(name = "ifr_min")            val ifrMin: Int,
    @ColumnInfo(name = "pic_min")            val picMin: Int,
    @ColumnInfo(name = "copilot_min")        val copilotMin: Int,
    @ColumnInfo(name = "dual_min")           val dualMin: Int,
    @ColumnInfo(name = "instructor_min")     val instructorMin: Int,

    // Operations
    @ColumnInfo(name = "takeoffs_day")       val takeoffsDay: Int,
    @ColumnInfo(name = "takeoffs_night")     val takeoffsNight: Int,
    @ColumnInfo(name = "landings_day")       val landingsDay: Int,
    @ColumnInfo(name = "landings_night")     val landingsNight: Int,
    @ColumnInfo(name = "is_multi_pilot")     val isMultiPilot: Boolean,

    // Crew
    @ColumnInfo(name = "pic_name", defaultValue = "")  val picName: String = "",

    // Commercial
    @ColumnInfo(name = "flight_number")      val flightNumber: String,
    @ColumnInfo(name = "remarks")            val remarks: String,

    // Logbook pagination
    @ColumnInfo(name = "page_break", defaultValue = "0") val pageBreak: Boolean = false,
)
