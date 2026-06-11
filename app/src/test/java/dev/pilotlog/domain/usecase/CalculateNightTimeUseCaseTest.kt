// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.pilotlog.domain.model.Airport
import dev.pilotlog.domain.usecase.nighttime.CalculateNightTimeUseCase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test

class CalculateNightTimeUseCaseTest {

    private val useCase = CalculateNightTimeUseCase()

    private val liege = Airport(
        icao = "EBLG", iata = "LGG", name = "Liège Airport",
        municipality = "Liège", country = "BE",
        latitude = 50.637, longitude = 5.443, elevationFt = 659,
        timezone = "Europe/Brussels",
    )
    private val paris = Airport(
        icao = "LFPG", iata = "CDG", name = "Paris Charles de Gaulle",
        municipality = "Paris", country = "FR",
        latitude = 49.009, longitude = 2.548, elevationFt = 392,
        timezone = "Europe/Paris",
    )

    @Test
    fun `full daytime flight returns zero night minutes`() {
        // LGG→CDG, 12:00→13:30 UTC in summer — well past sunrise, before sunset
        val result = useCase.execute(
            depAirport = liege,
            arrAirport = paris,
            date       = LocalDate(2026, 6, 15),
            depTime    = LocalTime(12, 0),
            arrTime    = LocalTime(13, 30),
        )
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `full night flight returns total flight time as night`() {
        // LGG→CDG, 01:00→02:30 UTC in June — well after sunset+30, before sunrise-30
        val result = useCase.execute(
            depAirport = liege,
            arrAirport = paris,
            date       = LocalDate(2026, 6, 15),
            depTime    = LocalTime(1, 0),
            arrTime    = LocalTime(2, 30),
        )
        // Expect close to 90 min (full flight is night)
        assertThat(result).isGreaterThan(80)
        assertThat(result).isAtMost(90)
    }

    @Test
    fun `overnight flight crossing midnight is handled correctly`() {
        // Departs 23:00, arrives 01:00 next day
        val result = useCase.execute(
            depAirport = liege,
            arrAirport = paris,
            date       = LocalDate(2026, 6, 15),
            depTime    = LocalTime(23, 0),
            arrTime    = LocalTime(1, 0),
        )
        assertThat(result).isGreaterThan(0)
        assertThat(result).isAtMost(120)
    }

    @Test
    fun `zero length flight returns zero`() {
        val result = useCase.execute(
            depAirport = liege,
            arrAirport = paris,
            date       = LocalDate(2026, 6, 15),
            depTime    = LocalTime(12, 0),
            arrTime    = LocalTime(12, 0),
        )
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `partial night flight returns partial minutes`() {
        // Flight from 20:00 to 22:00 UTC in June — partial night (sunset ~19:00 UTC + 30min)
        val result = useCase.execute(
            depAirport = liege,
            arrAirport = paris,
            date       = LocalDate(2026, 6, 15),
            depTime    = LocalTime(20, 0),
            arrTime    = LocalTime(22, 0),
        )
        assertThat(result).isGreaterThan(0)
        assertThat(result).isLessThan(120)
    }
}
