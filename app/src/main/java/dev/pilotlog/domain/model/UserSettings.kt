// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.model

/** The pilot's primary logbook role, used as the default for new flights. */
enum class PilotRole { PIC, COPILOT, DUAL, INSTRUCTOR }

/** How dates are rendered throughout the app. */
enum class DateFormat(val label: String, val example: String) {
    DMY("DD-MM-YYYY", "31-12-2026"),
    YMD("YYYY-MM-DD", "2026-12-31"),
}

/** App-wide user preferences (single record). */
data class UserSettings(
    val pilotName: String = "",
    val defaultRole: PilotRole = PilotRole.PIC,
    val homeBase: String = "",      // ICAO
    val dateFormat: DateFormat = DateFormat.DMY,
)
