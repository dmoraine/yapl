// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.pilotlog.data.database.entity.FlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Query("SELECT * FROM flights ORDER BY date DESC, dep_time DESC")
    fun getAll(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE date BETWEEN :from AND :to ORDER BY date DESC, dep_time DESC")
    fun getByDateRange(from: String, to: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FlightEntity?

    @Query("SELECT * FROM flights ORDER BY date DESC, arr_time DESC LIMIT 1")
    suspend fun getMostRecent(): FlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flight: FlightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flights: List<FlightEntity>)

    @Update
    suspend fun update(flight: FlightEntity)

    @Query("DELETE FROM flights WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM flights")
    suspend fun deleteAll()

    @Query("""
        SELECT
            COALESCE(SUM(total_min), 0)       AS totalMinutes,
            COALESCE(SUM(night_min), 0)       AS nightMinutes,
            COALESCE(SUM(ifr_min), 0)         AS ifrMinutes,
            COALESCE(SUM(pic_min), 0)         AS picMinutes,
            COALESCE(SUM(copilot_min), 0)     AS copilotMinutes,
            COALESCE(SUM(dual_min), 0)        AS dualMinutes,
            COALESCE(SUM(instructor_min), 0)  AS instructorMinutes,
            COUNT(*)                          AS totalFlights,
            COALESCE(SUM(landings_day + landings_night), 0) AS totalLandings,
            COALESCE(SUM(takeoffs_day + takeoffs_night), 0) AS totalTakeoffs
        FROM flights
        WHERE (:from IS NULL OR date >= :from)
          AND (:to   IS NULL OR date <= :to)
    """)
    suspend fun getStats(from: String?, to: String?): StatsRow

    data class StatsRow(
        val totalMinutes: Int,
        val nightMinutes: Int,
        val ifrMinutes: Int,
        val picMinutes: Int,
        val copilotMinutes: Int,
        val dualMinutes: Int,
        val instructorMinutes: Int,
        val totalFlights: Int,
        val totalLandings: Int,
        val totalTakeoffs: Int,
    )
}
