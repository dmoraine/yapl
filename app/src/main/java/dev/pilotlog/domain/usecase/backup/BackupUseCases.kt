// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.backup

import android.content.Context
import android.net.Uri
import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.Airport
import dev.pilotlog.domain.model.DateFormat
import dev.pilotlog.domain.model.EngineType
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.model.PilotRole
import dev.pilotlog.domain.model.PreviousTotals
import dev.pilotlog.domain.model.UserSettings
import dev.pilotlog.domain.repository.AircraftRepository
import dev.pilotlog.domain.repository.AirportRepository
import dev.pilotlog.domain.repository.FlightRepository
import dev.pilotlog.domain.usecase.previoustotals.GetPreviousTotalsUseCase
import dev.pilotlog.domain.usecase.previoustotals.SavePreviousTotalsUseCase
import dev.pilotlog.domain.usecase.settings.GetSettingsUseCase
import dev.pilotlog.domain.usecase.settings.SaveSettingsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/** Counts returned by the reference (airports + aircraft + setup) backup operations. */
data class ReferenceCounts(
    val airports: Int,
    val types: Int,
    val registrations: Int,
    val previousTotals: Boolean = false,
    val settings: Boolean = false,
)

// ── Flights CSV ──────────────────────────────────────────────────────────────────

private val FLIGHT_HEADER = listOf(
    "id", "date", "departure", "departure_time", "arrival", "arrival_time",
    "type", "registration", "total_min", "night_min", "ifr_min",
    "pic_min", "copilot_min", "dual_min", "instructor_min",
    "to_day", "to_night", "ldg_day", "ldg_night", "multi_pilot",
    "pic_name", "flight_number", "remarks", "page_break",
)

private fun String.csvEscape(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else this

/** Parse full CSV text into rows, honouring quoted fields (incl. embedded newlines). */
private fun parseCsv(text: String): List<List<String>> {
    val rows = ArrayList<List<String>>()
    var row = ArrayList<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes -> when {
                c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = false
                else -> sb.append(c)
            }
            c == '"' -> inQuotes = true
            c == ',' -> { row.add(sb.toString()); sb.setLength(0) }
            c == '\n' -> { row.add(sb.toString()); sb.setLength(0); rows.add(row); row = ArrayList() }
            c == '\r' -> { /* handled with \n */ }
            else -> sb.append(c)
        }
        i++
    }
    if (sb.isNotEmpty() || row.isNotEmpty()) { row.add(sb.toString()); rows.add(row) }
    return rows
}

class ExportFlightsCsvUseCase @Inject constructor(
    private val flightRepository: FlightRepository,
) {
    suspend operator fun invoke(context: Context, uri: Uri): Int {
        val flights = flightRepository.getFlights().first()
        val sb = StringBuilder()
        sb.append(FLIGHT_HEADER.joinToString(",")).append('\n')
        for (f in flights) {
            sb.append(
                listOf(
                    f.id.toString(),
                    f.date.toString(),
                    f.departureAirport,
                    f.departureTime.toString(),
                    f.arrivalAirport,
                    f.arrivalTime.toString(),
                    f.aircraftType,
                    f.aircraftRegistration,
                    f.totalMinutes.toString(),
                    f.nightMinutes.toString(),
                    f.ifrMinutes.toString(),
                    f.picMinutes.toString(),
                    f.copilotMinutes.toString(),
                    f.dualMinutes.toString(),
                    f.instructorMinutes.toString(),
                    f.takeoffsDay.toString(),
                    f.takeoffsNight.toString(),
                    f.landingsDay.toString(),
                    f.landingsNight.toString(),
                    f.isMultiPilot.toString(),
                    f.picName,
                    f.flightNumber,
                    f.remarks,
                    f.pageBreak.toString(),
                ).joinToString(",") { it.csvEscape() },
            ).append('\n')
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.bufferedWriter().use { it.write(sb.toString()) }
        } ?: throw IllegalStateException("Could not open file for writing")
        return flights.size
    }
}

class ImportFlightsCsvUseCase @Inject constructor(
    private val flightRepository: FlightRepository,
) {
    suspend operator fun invoke(context: Context, uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: return 0

        val rows = parseCsv(text)
        if (rows.isEmpty()) return 0

        val header = rows.first().map { it.trim() }
        val idx = header.withIndex().associate { (i, name) -> name to i }
        fun col(r: List<String>, name: String): String =
            idx[name]?.let { r.getOrNull(it) }?.trim() ?: ""

        val flights = rows.drop(1)
            .filter { r -> r.any { it.isNotBlank() } }
            .mapNotNull { r ->
                try {
                    Flight(
                        id = col(r, "id").toLongOrNull() ?: 0L,
                        date = LocalDate.parse(col(r, "date")),
                        departureAirport = col(r, "departure").uppercase(),
                        departureTime = LocalTime.parse(col(r, "departure_time")),
                        arrivalAirport = col(r, "arrival").uppercase(),
                        arrivalTime = LocalTime.parse(col(r, "arrival_time")),
                        aircraftType = col(r, "type").uppercase(),
                        aircraftRegistration = col(r, "registration").uppercase(),
                        totalMinutes = col(r, "total_min").toIntOrNull() ?: 0,
                        nightMinutes = col(r, "night_min").toIntOrNull() ?: 0,
                        ifrMinutes = col(r, "ifr_min").toIntOrNull() ?: 0,
                        picMinutes = col(r, "pic_min").toIntOrNull() ?: 0,
                        copilotMinutes = col(r, "copilot_min").toIntOrNull() ?: 0,
                        dualMinutes = col(r, "dual_min").toIntOrNull() ?: 0,
                        instructorMinutes = col(r, "instructor_min").toIntOrNull() ?: 0,
                        takeoffsDay = col(r, "to_day").toIntOrNull() ?: 0,
                        takeoffsNight = col(r, "to_night").toIntOrNull() ?: 0,
                        landingsDay = col(r, "ldg_day").toIntOrNull() ?: 0,
                        landingsNight = col(r, "ldg_night").toIntOrNull() ?: 0,
                        isMultiPilot = col(r, "multi_pilot").toBooleanStrictOrNull() ?: true,
                        picName = col(r, "pic_name"),
                        flightNumber = col(r, "flight_number"),
                        remarks = col(r, "remarks"),
                        pageBreak = col(r, "page_break").toBooleanStrictOrNull() ?: false,
                    )
                } catch (e: Exception) {
                    null
                }
            }

        if (flights.isNotEmpty()) flightRepository.insertAll(flights)
        return flights.size
    }
}

// ── Reference JSON (custom airports + aircraft types + registrations) ─────────────

class ExportReferenceJsonUseCase @Inject constructor(
    private val airportRepository: AirportRepository,
    private val aircraftRepository: AircraftRepository,
    private val getPreviousTotals: GetPreviousTotalsUseCase,
    private val getSettings: GetSettingsUseCase,
) {
    suspend operator fun invoke(context: Context, uri: Uri): ReferenceCounts {
        val airports = airportRepository.getCustomAirports()
        val types = aircraftRepository.getTypes().first()
        val registrations = aircraftRepository.getAllRegistrations().first()
        val previous = getPreviousTotals()
        val settings = getSettings()

        val airportsArr = JSONArray()
        for (a in airports) {
            airportsArr.put(
                JSONObject().apply {
                    put("icao", a.icao)
                    put("iata", a.iata)
                    put("name", a.name)
                    put("municipality", a.municipality)
                    put("country", a.country)
                    put("latitude", a.latitude ?: JSONObject.NULL)
                    put("longitude", a.longitude ?: JSONObject.NULL)
                    put("elevationFt", a.elevationFt ?: JSONObject.NULL)
                    put("timezone", a.timezone)
                },
            )
        }

        val typesArr = JSONArray()
        for (t in types) {
            typesArr.put(
                JSONObject().apply {
                    put("typeCode", t.typeCode)
                    put("typeName", t.typeName)
                    put("engineType", t.engineType.name)
                },
            )
        }

        val regsArr = JSONArray()
        for (r in registrations) {
            regsArr.put(
                JSONObject().apply {
                    put("registration", r.registration)
                    put("typeCode", r.typeCode)
                },
            )
        }

        val previousObj = previous?.let { p ->
            JSONObject().apply {
                put("asOf", p.asOf.toString())
                put("totalMinutes", p.totalMinutes)
                put("nightMinutes", p.nightMinutes)
                put("ifrMinutes", p.ifrMinutes)
                put("picMinutes", p.picMinutes)
                put("copilotMinutes", p.copilotMinutes)
                put("dualMinutes", p.dualMinutes)
                put("instructorMinutes", p.instructorMinutes)
                put("totalLandingsDay", p.totalLandingsDay)
                put("totalLandingsNight", p.totalLandingsNight)
                put("totalTakeoffs", p.totalTakeoffs)
            }
        } ?: JSONObject.NULL

        val settingsObj = JSONObject().apply {
            put("pilotName", settings.pilotName)
            put("defaultRole", settings.defaultRole.name)
            put("homeBase", settings.homeBase)
            put("dateFormat", settings.dateFormat.name)
        }

        val root = JSONObject().apply {
            put("version", 2)
            put("airports", airportsArr)
            put("aircraftTypes", typesArr)
            put("registrations", regsArr)
            put("previousTotals", previousObj)
            put("settings", settingsObj)
        }

        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.bufferedWriter().use { it.write(root.toString(2)) }
        } ?: throw IllegalStateException("Could not open file for writing")

        return ReferenceCounts(
            airports.size, types.size, registrations.size,
            previousTotals = previous != null, settings = true,
        )
    }
}

class ImportReferenceJsonUseCase @Inject constructor(
    private val airportRepository: AirportRepository,
    private val aircraftRepository: AircraftRepository,
    private val savePreviousTotals: SavePreviousTotalsUseCase,
    private val saveSettings: SaveSettingsUseCase,
) {
    suspend operator fun invoke(context: Context, uri: Uri): ReferenceCounts {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: return ReferenceCounts(0, 0, 0)

        val root = JSONObject(text)

        // Aircraft types first so registrations have a parent type to reference.
        val typesArr = root.optJSONArray("aircraftTypes") ?: JSONArray()
        var typeCount = 0
        for (i in 0 until typesArr.length()) {
            val o = typesArr.getJSONObject(i)
            val code = o.optString("typeCode").trim().uppercase()
            if (code.isBlank()) continue
            aircraftRepository.saveType(
                AircraftType(
                    typeCode = code,
                    typeName = o.optString("typeName"),
                    engineType = runCatching { EngineType.valueOf(o.optString("engineType")) }
                        .getOrDefault(EngineType.MULTI),
                ),
            )
            typeCount++
        }

        val regsArr = root.optJSONArray("registrations") ?: JSONArray()
        var regCount = 0
        for (i in 0 until regsArr.length()) {
            val o = regsArr.getJSONObject(i)
            val reg = o.optString("registration").trim().uppercase()
            val code = o.optString("typeCode").trim().uppercase()
            if (reg.isBlank() || code.isBlank()) continue
            aircraftRepository.saveRegistration(Aircraft(registration = reg, typeCode = code))
            regCount++
        }

        val airportsArr = root.optJSONArray("airports") ?: JSONArray()
        var airportCount = 0
        for (i in 0 until airportsArr.length()) {
            val o = airportsArr.getJSONObject(i)
            val icao = o.optString("icao").trim().uppercase()
            if (icao.isBlank()) continue
            airportRepository.saveCustomAirport(
                Airport(
                    icao = icao,
                    iata = o.optString("iata"),
                    name = o.optString("name"),
                    municipality = o.optString("municipality"),
                    country = o.optString("country"),
                    latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
                    longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
                    elevationFt = if (o.isNull("elevationFt")) null else o.optInt("elevationFt"),
                    timezone = o.optString("timezone", "UTC"),
                    isCustom = true,
                ),
            )
            airportCount++
        }

        // Previous totals (optional — absent in v1 files).
        val prevObj = root.optJSONObject("previousTotals")
        var prevImported = false
        if (prevObj != null) {
            savePreviousTotals(
                PreviousTotals(
                    asOf = runCatching { LocalDate.parse(prevObj.optString("asOf")) }
                        .getOrDefault(Clock.System.todayIn(TimeZone.UTC)),
                    totalMinutes = prevObj.optInt("totalMinutes"),
                    nightMinutes = prevObj.optInt("nightMinutes"),
                    ifrMinutes = prevObj.optInt("ifrMinutes"),
                    picMinutes = prevObj.optInt("picMinutes"),
                    copilotMinutes = prevObj.optInt("copilotMinutes"),
                    dualMinutes = prevObj.optInt("dualMinutes"),
                    instructorMinutes = prevObj.optInt("instructorMinutes"),
                    // Day falls back to the legacy combined "totalLandings" key (older exports).
                    totalLandingsDay = prevObj.optInt("totalLandingsDay", prevObj.optInt("totalLandings")),
                    totalLandingsNight = prevObj.optInt("totalLandingsNight"),
                    totalTakeoffs = prevObj.optInt("totalTakeoffs"),
                ),
            )
            prevImported = true
        }

        // Settings (optional — absent in v1 files).
        val setObj = root.optJSONObject("settings")
        var settingsImported = false
        if (setObj != null) {
            saveSettings(
                UserSettings(
                    pilotName = setObj.optString("pilotName"),
                    defaultRole = runCatching { PilotRole.valueOf(setObj.optString("defaultRole")) }
                        .getOrDefault(PilotRole.PIC),
                    homeBase = setObj.optString("homeBase"),
                    dateFormat = runCatching { DateFormat.valueOf(setObj.optString("dateFormat")) }
                        .getOrDefault(DateFormat.DMY),
                ),
            )
            settingsImported = true
        }

        return ReferenceCounts(airportCount, typeCount, regCount, prevImported, settingsImported)
    }
}
