// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.pilotlog.data.database.entity.AirportEntity

@Dao
interface AirportDao {

    @Query("""
        SELECT * FROM airports
        WHERE icao LIKE :query || '%'
           OR iata LIKE :query || '%'
           OR name LIKE '%' || :query || '%'
           OR municipality LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN icao = :query THEN 0
                 WHEN iata = :query THEN 1
                 WHEN icao LIKE :query || '%' THEN 2
                 WHEN iata LIKE :query || '%' THEN 3
                 ELSE 4 END,
            -- Prioritise airports with coordinates (needed for night-time calc)
            CASE WHEN latitude IS NOT NULL THEN 0 ELSE 1 END,
            name
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<AirportEntity>

    @Query("SELECT * FROM airports WHERE is_custom = 1 ORDER BY icao")
    suspend fun getCustom(): List<AirportEntity>

    @Query("SELECT * FROM airports WHERE icao = :icao LIMIT 1")
    suspend fun getByIcao(icao: String): AirportEntity?

    @Query("SELECT * FROM airports WHERE iata = :iata LIMIT 1")
    suspend fun getByIata(iata: String): AirportEntity?

    /** Upsert for user-created or user-edited airports. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(airport: AirportEntity)

    /** Bulk upsert for initial DB population from asset JSON. Skips custom entries. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoreConflicts(airports: List<AirportEntity>)

    @Query("DELETE FROM airports WHERE icao = :icao AND is_custom = 1")
    suspend fun deleteCustom(icao: String)
}
