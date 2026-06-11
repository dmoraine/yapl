// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.previoustotals

import dev.pilotlog.domain.model.PreviousTotals
import dev.pilotlog.domain.repository.PreviousTotalsRepository
import javax.inject.Inject

class SavePreviousTotalsUseCase @Inject constructor(
    private val repository: PreviousTotalsRepository,
) {
    suspend operator fun invoke(totals: PreviousTotals) = repository.save(totals)
}
