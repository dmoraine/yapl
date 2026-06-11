// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.model

import kotlinx.datetime.LocalDate

data class SimSession(
    val id: Long = 0,
    val date: LocalDate,
    val deviceType: String,
    val totalMinutes: Int,
    val remarks: String = "",
)
