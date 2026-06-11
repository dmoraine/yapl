// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pilotlog.data.repository.AircraftRepositoryImpl
import dev.pilotlog.data.repository.AirportRepositoryImpl
import dev.pilotlog.data.repository.FlightRepositoryImpl
import dev.pilotlog.data.repository.PreviousTotalsRepositoryImpl
import dev.pilotlog.data.repository.SettingsRepositoryImpl
import dev.pilotlog.domain.repository.AircraftRepository
import dev.pilotlog.domain.repository.AirportRepository
import dev.pilotlog.domain.repository.FlightRepository
import dev.pilotlog.domain.repository.PreviousTotalsRepository
import dev.pilotlog.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindFlightRepository(impl: FlightRepositoryImpl): FlightRepository

    @Binds @Singleton
    abstract fun bindAircraftRepository(impl: AircraftRepositoryImpl): AircraftRepository

    @Binds @Singleton
    abstract fun bindAirportRepository(impl: AirportRepositoryImpl): AirportRepository

    @Binds @Singleton
    abstract fun bindPreviousTotalsRepository(impl: PreviousTotalsRepositoryImpl): PreviousTotalsRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
