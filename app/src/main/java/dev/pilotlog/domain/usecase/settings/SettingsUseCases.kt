// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.settings

import dev.pilotlog.domain.model.UserSettings
import dev.pilotlog.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<UserSettings> = repository.observe()
}

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(): UserSettings = repository.get()
}

class SaveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(settings: UserSettings) = repository.save(settings)
}
