// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.repository

import dev.pilotlog.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<UserSettings>
    suspend fun get(): UserSettings
    suspend fun save(settings: UserSettings)
}
