// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.repository

import dev.pilotlog.data.database.dao.AppSettingsDao
import dev.pilotlog.data.database.entity.AppSettingsEntity
import dev.pilotlog.domain.model.DateFormat
import dev.pilotlog.domain.model.PilotRole
import dev.pilotlog.domain.model.UserSettings
import dev.pilotlog.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingsDao,
) : SettingsRepository {

    override fun observe(): Flow<UserSettings> =
        dao.observe().map { it?.toDomain() ?: UserSettings() }

    override suspend fun get(): UserSettings =
        dao.get()?.toDomain() ?: UserSettings()

    override suspend fun save(settings: UserSettings) =
        dao.upsert(settings.toEntity())

    private fun AppSettingsEntity.toDomain() = UserSettings(
        pilotName   = pilotName,
        defaultRole = runCatching { PilotRole.valueOf(defaultRole) }.getOrDefault(PilotRole.PIC),
        homeBase    = homeBase,
        dateFormat  = runCatching { DateFormat.valueOf(dateFormat) }.getOrDefault(DateFormat.DMY),
    )

    private fun UserSettings.toEntity() = AppSettingsEntity(
        id          = 1,
        pilotName   = pilotName,
        defaultRole = defaultRole.name,
        homeBase    = homeBase,
        dateFormat  = dateFormat.name,
    )
}
