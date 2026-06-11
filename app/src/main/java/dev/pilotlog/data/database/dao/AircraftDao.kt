// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.pilotlog.data.database.entity.AircraftEntity
import dev.pilotlog.data.database.entity.AircraftTypeEntity
import dev.pilotlog.data.database.entity.TypeWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface AircraftDao {

    // ── Types ──────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM aircraft_types ORDER BY type_code")
    fun getTypes(): Flow<List<AircraftTypeEntity>>

    @Query(
        """
        SELECT t.type_code AS type_code, t.type_name AS type_name, t.engine_type AS engine_type,
               (SELECT COUNT(*) FROM aircraft a WHERE a.type_code = t.type_code) AS reg_count
        FROM aircraft_types t
        ORDER BY reg_count DESC, t.type_code
        """
    )
    fun getTypesWithCounts(): Flow<List<TypeWithCount>>

    @Query("SELECT * FROM aircraft_types WHERE type_code = :code LIMIT 1")
    suspend fun getType(code: String): AircraftTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertType(type: AircraftTypeEntity)

    @Query("DELETE FROM aircraft_types WHERE type_code = :code")
    suspend fun deleteType(code: String)

    // ── Registrations ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM aircraft WHERE type_code = :typeCode ORDER BY registration")
    fun getRegistrationsByType(typeCode: String): Flow<List<AircraftEntity>>

    @Query("SELECT * FROM aircraft ORDER BY registration")
    fun getAllRegistrations(): Flow<List<AircraftEntity>>

    @Query("SELECT * FROM aircraft WHERE registration = :registration LIMIT 1")
    suspend fun getByRegistration(registration: String): AircraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegistration(aircraft: AircraftEntity)

    @Query("DELETE FROM aircraft WHERE registration = :registration")
    suspend fun deleteRegistration(registration: String)
}
