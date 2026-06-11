// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,                  // Single-row table — always id=1
    @ColumnInfo(name = "pilot_name")   val pilotName: String = "",
    @ColumnInfo(name = "default_role") val defaultRole: String = "PIC",
    @ColumnInfo(name = "home_base")    val homeBase: String = "",
    @ColumnInfo(name = "date_format", defaultValue = "DMY") val dateFormat: String = "DMY",
)
