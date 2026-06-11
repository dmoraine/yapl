// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.mapper

import dev.pilotlog.data.database.entity.FlightEntity
import dev.pilotlog.domain.model.Flight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun FlightEntity.toDomain(): Flight = Flight(
    id                  = id,
    date                = LocalDate.parse(date),
    departureAirport    = depAirport,
    departureTime       = LocalTime.parse(depTime),
    arrivalAirport      = arrAirport,
    arrivalTime         = LocalTime.parse(arrTime),
    aircraftType        = acType,
    aircraftRegistration = acRegistration,
    totalMinutes        = totalMin,
    nightMinutes        = nightMin,
    ifrMinutes          = ifrMin,
    picMinutes          = picMin,
    copilotMinutes      = copilotMin,
    dualMinutes         = dualMin,
    instructorMinutes   = instructorMin,
    takeoffsDay         = takeoffsDay,
    takeoffsNight       = takeoffsNight,
    landingsDay         = landingsDay,
    landingsNight       = landingsNight,
    isMultiPilot        = isMultiPilot,
    picName             = picName,
    flightNumber        = flightNumber,
    remarks             = remarks,
    pageBreak           = pageBreak,
)

fun Flight.toEntity(): FlightEntity = FlightEntity(
    id                  = id,
    date                = date.toString(),
    depAirport          = departureAirport,
    depTime             = departureTime.toString(),
    arrAirport          = arrivalAirport,
    arrTime             = arrivalTime.toString(),
    acType              = aircraftType,
    acRegistration      = aircraftRegistration,
    totalMin            = totalMinutes,
    nightMin            = nightMinutes,
    ifrMin              = ifrMinutes,
    picMin              = picMinutes,
    copilotMin          = copilotMinutes,
    dualMin             = dualMinutes,
    instructorMin       = instructorMinutes,
    takeoffsDay         = takeoffsDay,
    takeoffsNight       = takeoffsNight,
    landingsDay         = landingsDay,
    landingsNight       = landingsNight,
    isMultiPilot        = isMultiPilot,
    picName             = picName,
    flightNumber        = flightNumber,
    remarks             = remarks,
    pageBreak           = pageBreak,
)
