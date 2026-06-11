// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.previoustotals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.PreviousTotals
import dev.pilotlog.domain.usecase.previoustotals.GetPreviousTotalsUseCase
import dev.pilotlog.domain.usecase.previoustotals.SavePreviousTotalsUseCase
import dev.pilotlog.ui.util.parseHhMm
import dev.pilotlog.ui.util.toHhMm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class PreviousTotalsFormState(
    val asOf: String = "",
    val totalTime: String = "",
    val nightTime: String = "",
    val ifrTime: String = "",
    val picTime: String = "",
    val copilotTime: String = "",
    val dualTime: String = "",
    val instructorTime: String = "",
    val landingsDay: String = "",
    val landingsNight: String = "",
    val takeoffs: String = "",
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
)

@HiltViewModel
class PreviousTotalsViewModel @Inject constructor(
    private val getPreviousTotals: GetPreviousTotalsUseCase,
    private val savePreviousTotals: SavePreviousTotalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PreviousTotalsFormState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = getPreviousTotals()
            if (existing != null) {
                _state.update {
                    it.copy(
                        asOf        = existing.asOf.toString(),
                        totalTime   = existing.totalMinutes.toHhMm(),
                        nightTime   = existing.nightMinutes.toHhMm(),
                        ifrTime     = existing.ifrMinutes.toHhMm(),
                        picTime     = existing.picMinutes.toHhMm(),
                        copilotTime = existing.copilotMinutes.toHhMm(),
                        dualTime    = existing.dualMinutes.toHhMm(),
                        instructorTime = existing.instructorMinutes.toHhMm(),
                        landingsDay   = existing.totalLandingsDay.toString(),
                        landingsNight = existing.totalLandingsNight.toString(),
                        takeoffs      = existing.totalTakeoffs.toString(),
                    )
                }
            } else {
                val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
                _state.update { it.copy(asOf = today.toString()) }
            }
        }
    }

    fun onFieldChange(field: PreviousTotalsField, value: String) {
        _state.update { s ->
            when (field) {
                PreviousTotalsField.AS_OF        -> s.copy(asOf = value)
                PreviousTotalsField.TOTAL        -> s.copy(totalTime = value)
                PreviousTotalsField.NIGHT        -> s.copy(nightTime = value)
                PreviousTotalsField.IFR          -> s.copy(ifrTime = value)
                PreviousTotalsField.PIC          -> s.copy(picTime = value)
                PreviousTotalsField.COPILOT      -> s.copy(copilotTime = value)
                PreviousTotalsField.DUAL         -> s.copy(dualTime = value)
                PreviousTotalsField.INSTRUCTOR   -> s.copy(instructorTime = value)
                PreviousTotalsField.LANDINGS_DAY   -> s.copy(landingsDay = value)
                PreviousTotalsField.LANDINGS_NIGHT -> s.copy(landingsNight = value)
                PreviousTotalsField.TAKEOFFS       -> s.copy(takeoffs = value)
            }
        }
    }

    fun save() {
        val s = _state.value
        val asOf = runCatching { kotlinx.datetime.LocalDate.parse(s.asOf) }.getOrNull() ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            savePreviousTotals(
                PreviousTotals(
                    asOf              = asOf,
                    totalMinutes      = parseHhMm(s.totalTime) ?: 0,
                    nightMinutes      = parseHhMm(s.nightTime) ?: 0,
                    ifrMinutes        = parseHhMm(s.ifrTime) ?: 0,
                    picMinutes        = parseHhMm(s.picTime) ?: 0,
                    copilotMinutes    = parseHhMm(s.copilotTime) ?: 0,
                    dualMinutes       = parseHhMm(s.dualTime) ?: 0,
                    instructorMinutes = parseHhMm(s.instructorTime) ?: 0,
                    totalLandingsDay   = s.landingsDay.toIntOrNull() ?: 0,
                    totalLandingsNight = s.landingsNight.toIntOrNull() ?: 0,
                    totalTakeoffs      = s.takeoffs.toIntOrNull() ?: 0,
                )
            )
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

enum class PreviousTotalsField {
    AS_OF, TOTAL, NIGHT, IFR, PIC, COPILOT, DUAL, INSTRUCTOR, LANDINGS_DAY, LANDINGS_NIGHT, TAKEOFFS
}
