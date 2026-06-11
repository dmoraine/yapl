// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.pilotlog.data.database.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingsEntity)
}
