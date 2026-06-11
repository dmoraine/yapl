// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.pilotlog.data.database.entity.PreviousTotalsEntity

@Dao
interface PreviousTotalsDao {

    @Query("SELECT * FROM previous_totals WHERE id = 1 LIMIT 1")
    suspend fun get(): PreviousTotalsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PreviousTotalsEntity)

    @Query("DELETE FROM previous_totals WHERE id = 1")
    suspend fun clear()
}
