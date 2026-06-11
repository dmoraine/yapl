// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.nighttime

import dev.pilotlog.domain.model.Airport
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes the aeronautical night portion of a flight.
 *
 * Aeronautical night starts 30 min after sunset and ends 30 min before sunrise
 * (SERA.M.465 / most national regulations).
 *
 * Algorithm:
 *  1. Compute departure and arrival instants in UTC.
 *  2. Sample the position along the great-circle route every [SAMPLE_MINUTES] minutes.
 *  3. At each sample, compute sunset/sunrise using the NOAA algorithm (accurate to ~1 min).
 *  4. Count samples that fall within the aeronautical night window.
 *
 * The NOAA sunrise/sunset algorithm is ported from:
 *   https://gml.noaa.gov/grad/solcalc/solareqns.PDF
 */
class CalculateNightTimeUseCase @Inject constructor() {

    private companion object {
        const val SAMPLE_MINUTES = 5
        const val NIGHT_OFFSET_MIN = 30
        const val DEG = PI / 180.0
    }

    /**
     * @param depAirport    Departure airport (used for lat/lon).
     * @param arrAirport    Arrival airport.
     * @param date          Date of departure (UTC).
     * @param depTime       Block-off time (UTC).
     * @param arrTime       Block-on time (UTC). If < depTime the arrival is the next day.
     * @return              Night time in whole minutes.
     */
    fun execute(
        depAirport: Airport,
        arrAirport: Airport,
        date: LocalDate,
        depTime: LocalTime,
        arrTime: LocalTime,
    ): Int {
        // Cannot compute night time without coordinates — return 0 so the user
        // can override manually (custom airports added without lat/lon).
        val depLat = depAirport.latitude ?: return 0
        val depLon = depAirport.longitude ?: return 0
        val arrLat = arrAirport.latitude ?: depLat
        val arrLon = arrAirport.longitude ?: depLon

        val tz = TimeZone.UTC
        val depInstant = LocalDateTime(date, depTime).toInstant(tz)

        // Handle overnight flights: if arrival is before departure, it's the next day.
        val arrDate = if (arrTime < depTime) date.nextDay() else date
        val arrInstant = LocalDateTime(arrDate, arrTime).toInstant(tz)

        val flightMinutes = (arrInstant - depInstant).inWholeMinutes.toInt()
        if (flightMinutes <= 0) return 0

        val samples = (flightMinutes / SAMPLE_MINUTES).coerceAtLeast(1)
        var nightCount = 0

        for (i in 0..samples) {
            val fraction = if (samples == 0) 0.5 else i.toDouble() / samples
            val offsetSeconds = (fraction * flightMinutes * 60).toLong()
            val sampleInstant = Instant.fromEpochSeconds(depInstant.epochSeconds + offsetSeconds)

            // Interpolate position along great circle
            val (lat, lon) = greatCircleInterpolate(
                lat1 = depLat, lon1 = depLon,
                lat2 = arrLat, lon2 = arrLon,
                fraction = fraction,
            )

            val sampleDate = sampleInstant.toLocalDateTime(tz).date
            val sunriseUtcMin = sunriseUtcMinutes(lat, lon, sampleDate)
            val sunsetUtcMin = sunsetUtcMinutes(lat, lon, sampleDate)

            val sampleMinOfDay = (sampleInstant.epochSeconds % 86400 / 60).toInt()

            if (isAeroNight(sampleMinOfDay, sunriseUtcMin, sunsetUtcMin)) {
                nightCount++
            }
        }

        // Each sample covers SAMPLE_MINUTES minutes; clamp to actual flight time.
        return (nightCount * SAMPLE_MINUTES).coerceAtMost(flightMinutes)
    }

    /**
     * Is the given UTC instant ([date] + [time]) within aeronautical night at [airport]?
     * Returns null when the airport has no coordinates (cannot be determined).
     */
    fun isNightAt(airport: Airport, date: LocalDate, time: LocalTime): Boolean? {
        val lat = airport.latitude ?: return null
        val lon = airport.longitude ?: return null
        val sunrise = sunriseUtcMinutes(lat, lon, date)
        val sunset = sunsetUtcMinutes(lat, lon, date)
        return isAeroNight(time.hour * 60 + time.minute, sunrise, sunset)
    }

    // ── Position interpolation ────────────────────────────────────────────────

    /** Linearly interpolates latitude/longitude (sufficient for logbook accuracy). */
    private fun greatCircleInterpolate(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        fraction: Double,
    ): Pair<Double, Double> = Pair(
        lat1 + (lat2 - lat1) * fraction,
        lon1 + (lon2 - lon1) * fraction,
    )

    // ── NOAA sunrise/sunset ───────────────────────────────────────────────────

    /**
     * Returns UTC time of sunrise in minutes from midnight, or null if the sun
     * never rises (polar night) or never sets (midnight sun).
     */
    private fun sunriseUtcMinutes(lat: Double, lon: Double, date: LocalDate): Int? =
        sunTransit(lat, lon, date, rising = true)

    private fun sunsetUtcMinutes(lat: Double, lon: Double, date: LocalDate): Int? =
        sunTransit(lat, lon, date, rising = false)

    private fun sunTransit(lat: Double, lon: Double, date: LocalDate, rising: Boolean): Int? {
        val jd = julianDay(date)
        val t = (jd - 2451545.0) / 36525.0

        val eqTime = equationOfTime(t)          // minutes
        val decl = solarDeclination(t)          // degrees

        val latRad = lat * DEG
        val declRad = decl * DEG

        // Hour angle at standard horizon (-0.833°)
        val cosH = (sin(-0.833 * DEG) - sin(latRad) * sin(declRad)) /
                (cos(latRad) * cos(declRad))

        if (cosH < -1.0) return null  // midnight sun
        if (cosH > 1.0) return null   // polar night

        val haDeg = Math.toDegrees(acos(cosH))
        // rising=true  → sunrise: ha_var = +haDeg → 720 - 4*(lon+HA) - eqTime = 720 - 4*lon - 4*HA - eqTime
        // rising=false → sunset:  ha_var = -haDeg → 720 - 4*(lon-HA) - eqTime = 720 - 4*lon + 4*HA - eqTime
        val ha = if (rising) haDeg else -haDeg

        // UTC time of transit in minutes
        val utcMin = (720.0 - 4.0 * (lon + ha) - eqTime).toInt()
        return utcMin.coerceIn(0, 1439)
    }

    private fun equationOfTime(t: Double): Double {
        val epsilon = obliquityCorrection(t) * DEG
        val l0 = geomMeanLongSun(t) * DEG
        val e = eccentricityEarth(t)
        val m = geomMeanAnomalySun(t) * DEG
        var y = Math.tan(epsilon / 2.0)
        y *= y
        val sinM = sin(m)
        val sin2L0 = sin(2.0 * l0)
        val cos2L0 = cos(2.0 * l0)
        val sin4L0 = sin(4.0 * l0)
        val sin2M = sin(2.0 * m)
        val eqTime = y * sin2L0 - 2.0 * e * sinM + 4.0 * e * y * sinM * cos2L0 -
                0.5 * y * y * sin4L0 - 1.25 * e * e * sin2M
        return Math.toDegrees(eqTime) * 4.0  // in minutes
    }

    private fun solarDeclination(t: Double): Double {
        val e = obliquityCorrection(t) * DEG
        val lambda = sunApparentLong(t) * DEG
        return Math.toDegrees(asin(sin(e) * sin(lambda)))
    }

    private fun sunApparentLong(t: Double): Double {
        val o = sunTrueLong(t)
        val omega = 125.04 - 1934.136 * t
        return o - 0.00569 - 0.00478 * sin(omega * DEG)
    }

    private fun sunTrueLong(t: Double): Double {
        val l0 = geomMeanLongSun(t)
        val c = sunEqOfCenter(t)
        return l0 + c
    }

    private fun sunEqOfCenter(t: Double): Double {
        val m = geomMeanAnomalySun(t) * DEG
        return sin(m) * (1.9146 - t * (0.004817 + 0.000014 * t)) +
                sin(2 * m) * (0.019993 - 0.000101 * t) +
                sin(3 * m) * 0.00029
    }

    private fun geomMeanLongSun(t: Double): Double = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360
    private fun geomMeanAnomalySun(t: Double): Double = 357.52911 + t * (35999.05029 - 0.0001537 * t)
    private fun eccentricityEarth(t: Double): Double = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    private fun obliquityCorrection(t: Double): Double {
        val e0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val omega = 125.04 - 1934.136 * t
        return e0 + 0.00256 * cos(omega * DEG)
    }

    private fun julianDay(date: LocalDate): Double {
        var y = date.year
        var m = date.monthNumber
        val d = date.dayOfMonth
        if (m <= 2) { y--; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + d + b - 1524.5
    }

    // ── Night determination ───────────────────────────────────────────────────

    /**
     * Returns true if [sampleMin] (UTC minutes from midnight) falls in aeronautical night.
     * Handles the case where night straddles midnight.
     */
    private fun isAeroNight(sampleMin: Int, sunriseMin: Int?, sunsetMin: Int?): Boolean {
        if (sunriseMin == null && sunsetMin == null) return false // midnight sun: no night
        if (sunriseMin == null || sunsetMin == null) return true  // polar night: all night

        val nightStart = (sunsetMin + NIGHT_OFFSET_MIN) % 1440
        val nightEnd = (sunriseMin - NIGHT_OFFSET_MIN + 1440) % 1440

        return if (nightStart < nightEnd) {
            sampleMin in nightStart until nightEnd
        } else {
            // Night straddles midnight
            sampleMin >= nightStart || sampleMin < nightEnd
        }
    }

    private fun LocalDate.nextDay(): LocalDate = plus(1, DateTimeUnit.DAY)

}
