// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.legacy

import android.content.Context
import android.net.Uri
import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.EngineType
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.repository.AircraftRepository
import dev.pilotlog.domain.repository.FlightRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class ImportResult(
    val flightsImported: Int,
    val aircraftImported: Int,
    val skipped: Int,
)

class ImportLegacyFlightsUseCase @Inject constructor(
    private val flightRepository: FlightRepository,
    private val aircraftRepository: AircraftRepository,
) {
    suspend operator fun invoke(context: Context, uri: Uri): ImportResult {
        val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: return ImportResult(0, 0, 0)

        val root = JSONObject(json)
        val events   = root.optJSONArray("events")   ?: return ImportResult(0, 0, 0)
        val models   = root.optJSONArray("acModels")  ?: JSONArray()
        val aircrafts = root.optJSONArray("aircrafts") ?: JSONArray()

        val acLookup = buildAircraftLookup(models, aircrafts)

        // Insert unique types, then registrations
        var aircraftInserted = 0
        val seenRegs = mutableSetOf<String>()
        val seenTypes = mutableSetOf<String>()
        for (acInfo in acLookup.values) {
            val type = acInfo.typeCode.trim().uppercase()
            if (type.isNotBlank() && type !in seenTypes) {
                seenTypes.add(type)
                if (aircraftRepository.getType(type) == null) {
                    aircraftRepository.saveType(
                        AircraftType(
                            typeCode   = type,
                            typeName   = acInfo.typeName,
                            engineType = acInfo.engineType,
                        ),
                    )
                }
            }
            val reg = acInfo.registration
            if (reg.isNotBlank() && reg !in seenRegs && type.isNotBlank()) {
                seenRegs.add(reg)
                if (aircraftRepository.getByRegistration(reg) == null) {
                    aircraftRepository.saveRegistration(
                        Aircraft(registration = reg, typeCode = type),
                    )
                    aircraftInserted++
                }
            }
        }

        // Map and bulk-insert flights
        val flights = mutableListOf<Flight>()
        var skipped = 0
        for (i in 0 until events.length()) {
            val evt = events.getJSONObject(i)
            if (evt.optInt("type", 0) != 0) { skipped++; continue }
            try {
                val flight = evt.toFlight(acLookup)
                flights.add(flight)
            } catch (e: Exception) {
                skipped++
            }
        }

        flightRepository.insertAll(flights)
        return ImportResult(flights.size, aircraftInserted, skipped)
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private data class AcInfo(
        val typeCode: String,
        val typeName: String,
        val registration: String,
        val engineType: EngineType,
    )

    private fun buildAircraftLookup(models: JSONArray, aircrafts: JSONArray): Map<Int, AcInfo> {
        val modelMap = mutableMapOf<Int, JSONObject>()
        for (i in 0 until models.length()) {
            val m = models.getJSONObject(i)
            modelMap[m.getInt("id")] = m
        }
        val result = mutableMapOf<Int, AcInfo>()
        for (i in 0 until aircrafts.length()) {
            val ac = aircrafts.getJSONObject(i)
            val modelId = ac.optString("modelId", "0").toIntOrNull() ?: 0
            val model = modelMap[modelId]
            result[ac.getInt("id")] = AcInfo(
                typeCode     = model?.optString("code", "") ?: "",
                typeName     = model?.optString("fullName", "") ?: "",
                registration = ac.optString("registration", "").trim().uppercase(),
                engineType   = if (model?.optBoolean("multiEngine", true) != false)
                    EngineType.MULTI else EngineType.SINGLE,
            )
        }
        return result
    }

    private fun JSONObject.toFlight(acLookup: Map<Int, AcInfo>): Flight {
        val epochMs = getLong("logDate")
        val date: LocalDate = Instant.fromEpochMilliseconds(epochMs)
            .toLocalDateTime(TimeZone.UTC)
            .date

        fun minToTime(min: Int) = LocalTime(min / 60 % 24, min % 60)

        val ac = acLookup[optInt("aircraftId")]

        return Flight(
            id = 0L,
            date = date,
            departureAirport = optString("depPlace", "").trim().uppercase(),
            departureTime    = minToTime(optInt("depTime")),
            arrivalAirport   = optString("arrPlace", "").trim().uppercase(),
            arrivalTime      = minToTime(optInt("arrTime")),
            aircraftType     = ac?.typeCode ?: "",
            aircraftRegistration = ac?.registration ?: "",
            totalMinutes     = optInt("length"),
            nightMinutes     = optInt("nightTime"),
            ifrMinutes       = optInt("ifrTime"),
            picMinutes       = optInt("picTime"),
            copilotMinutes   = optInt("coPilotTime"),
            dualMinutes      = optInt("dualTime"),
            instructorMinutes = optInt("instructorTime"),
            takeoffsDay      = optInt("takeoffDay"),
            takeoffsNight    = optInt("takeoffNight"),
            landingsDay      = optInt("landingDay"),
            landingsNight    = optInt("landingNight"),
            isMultiPilot     = optInt("multiPilotTime") > 0,
            flightNumber     = optString("flightNumb", ""),
            remarks          = optString("remarks", ""),
        )
    }
}
