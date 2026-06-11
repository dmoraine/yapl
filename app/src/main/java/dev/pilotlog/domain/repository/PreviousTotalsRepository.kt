// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.repository

import dev.pilotlog.domain.model.PreviousTotals

interface PreviousTotalsRepository {
    suspend fun get(): PreviousTotals?
    suspend fun save(totals: PreviousTotals)
    suspend fun clear()
}
