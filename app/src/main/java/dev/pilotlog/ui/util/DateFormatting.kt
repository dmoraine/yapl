// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.util

import androidx.compose.runtime.compositionLocalOf
import dev.pilotlog.domain.model.DateFormat
import kotlinx.datetime.LocalDate

/**
 * The user's chosen date format, provided once at the app root from settings so
 * any screen can read it without threading it through every ViewModel.
 */
val LocalDateFormat = compositionLocalOf { DateFormat.DMY }

/** Render a date according to the user's preferred format. */
fun LocalDate.formatted(format: DateFormat): String = when (format) {
    DateFormat.DMY -> "%02d-%02d-%04d".format(dayOfMonth, monthNumber, year)
    DateFormat.YMD -> "%04d-%02d-%02d".format(year, monthNumber, dayOfMonth)
}
