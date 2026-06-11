// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pilotlog.data.database.dao.AircraftDao
import dev.pilotlog.data.database.dao.AirportDao
import dev.pilotlog.data.database.dao.AppSettingsDao
import dev.pilotlog.data.database.dao.FlightDao
import dev.pilotlog.data.database.dao.PreviousTotalsDao
import dev.pilotlog.data.database.entity.AircraftEntity
import dev.pilotlog.data.database.entity.AircraftTypeEntity
import dev.pilotlog.data.database.entity.AirportEntity
import dev.pilotlog.data.database.entity.AppSettingsEntity
import dev.pilotlog.data.database.entity.FlightEntity
import dev.pilotlog.data.database.entity.PreviousTotalsEntity

@Database(
    entities = [
        FlightEntity::class,
        AircraftEntity::class,
        AircraftTypeEntity::class,
        AirportEntity::class,
        PreviousTotalsEntity::class,
        AppSettingsEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class PilotLogDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun aircraftDao(): AircraftDao
    abstract fun airportDao(): AirportDao
    abstract fun previousTotalsDao(): PreviousTotalsDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        const val DATABASE_NAME = "pilotlog.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS previous_totals (
                        id             INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                        as_of          TEXT    NOT NULL,
                        total_min      INTEGER NOT NULL DEFAULT 0,
                        night_min      INTEGER NOT NULL DEFAULT 0,
                        ifr_min        INTEGER NOT NULL DEFAULT 0,
                        pic_min        INTEGER NOT NULL DEFAULT 0,
                        copilot_min    INTEGER NOT NULL DEFAULT 0,
                        dual_min       INTEGER NOT NULL DEFAULT 0,
                        instructor_min INTEGER NOT NULL DEFAULT 0,
                        total_landings INTEGER NOT NULL DEFAULT 0,
                        total_takeoffs INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        /**
         * Split the flat `aircraft` table into a two-level model:
         *  - `aircraft_types`  (type_code PK, type_name, engine_type)
         *  - `aircraft`        (registration PK, type_code FK)
         * Existing rows are preserved by deriving types from distinct codes.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS aircraft_types (
                        type_code   TEXT NOT NULL PRIMARY KEY,
                        type_name   TEXT NOT NULL,
                        engine_type TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """INSERT OR IGNORE INTO aircraft_types (type_code, type_name, engine_type)
                       SELECT type_code, MAX(type_name), MAX(engine_type)
                       FROM aircraft GROUP BY type_code"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS aircraft_new (
                        registration TEXT NOT NULL PRIMARY KEY,
                        type_code    TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """INSERT OR IGNORE INTO aircraft_new (registration, type_code)
                       SELECT registration, type_code FROM aircraft"""
                )
                db.execSQL("DROP TABLE aircraft")
                db.execSQL("ALTER TABLE aircraft_new RENAME TO aircraft")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_aircraft_type_code ON aircraft(type_code)")
            }
        }

        /** Add per-flight PIC name + single-row app_settings (pilot name, default role, home base). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flights ADD COLUMN pic_name TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS app_settings (
                        id           INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                        pilot_name   TEXT NOT NULL DEFAULT '',
                        default_role TEXT NOT NULL DEFAULT 'PIC',
                        home_base    TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        /** Add user-selectable date display format to app_settings. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN date_format TEXT NOT NULL DEFAULT 'DMY'")
            }
        }

        /** Add per-flight paper-logbook page-break marker. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flights ADD COLUMN page_break INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Split previous-totals landings into day/night (existing total_landings becomes day). */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE previous_totals ADD COLUMN total_landings_night INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
