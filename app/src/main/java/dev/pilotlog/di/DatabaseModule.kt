// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pilotlog.data.database.PilotLogDatabase
import dev.pilotlog.data.database.dao.AircraftDao
import dev.pilotlog.data.database.dao.AirportDao
import dev.pilotlog.data.database.dao.AppSettingsDao
import dev.pilotlog.data.database.dao.FlightDao
import dev.pilotlog.data.database.dao.PreviousTotalsDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PilotLogDatabase =
        Room.databaseBuilder(
            context,
            PilotLogDatabase::class.java,
            PilotLogDatabase.DATABASE_NAME,
        )
            // Airports are pre-populated from a bundled SQLite asset.
            // On first install the asset is copied; subsequent opens use the live DB.
            .createFromAsset("airports.db")
            .addMigrations(
                PilotLogDatabase.MIGRATION_1_2,
                PilotLogDatabase.MIGRATION_2_3,
                PilotLogDatabase.MIGRATION_3_4,
                PilotLogDatabase.MIGRATION_4_5,
                PilotLogDatabase.MIGRATION_5_6,
                PilotLogDatabase.MIGRATION_6_7,
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideFlightDao(db: PilotLogDatabase): FlightDao = db.flightDao()

    @Provides
    fun provideAircraftDao(db: PilotLogDatabase): AircraftDao = db.aircraftDao()

    @Provides
    fun provideAirportDao(db: PilotLogDatabase): AirportDao = db.airportDao()

    @Provides
    fun providePreviousTotalsDao(db: PilotLogDatabase): PreviousTotalsDao = db.previousTotalsDao()

    @Provides
    fun provideAppSettingsDao(db: PilotLogDatabase): AppSettingsDao = db.appSettingsDao()
}
