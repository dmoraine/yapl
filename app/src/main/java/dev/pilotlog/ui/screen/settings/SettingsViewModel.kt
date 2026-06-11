// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.Airport
import dev.pilotlog.domain.model.DateFormat
import dev.pilotlog.domain.model.PilotRole
import dev.pilotlog.domain.model.UserSettings
import dev.pilotlog.domain.usecase.airport.SearchAirportsUseCase
import dev.pilotlog.domain.usecase.backup.ExportFlightsCsvUseCase
import dev.pilotlog.domain.usecase.backup.ExportLogbookPdfUseCase
import dev.pilotlog.domain.usecase.backup.ExportReferenceJsonUseCase
import dev.pilotlog.domain.usecase.backup.ImportFlightsCsvUseCase
import dev.pilotlog.domain.usecase.backup.ImportReferenceJsonUseCase
import dev.pilotlog.domain.usecase.legacy.ImportLegacyFlightsUseCase
import dev.pilotlog.domain.usecase.legacy.ImportResult
import dev.pilotlog.domain.usecase.maintenance.ClearAllFlightsUseCase
import dev.pilotlog.domain.usecase.settings.GetSettingsUseCase
import dev.pilotlog.domain.usecase.settings.SaveSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val homeBaseQuery: String = "",
    val homeBaseSuggestions: List<Airport> = emptyList(),
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null,
    val importError: String? = null,
    val isBackupBusy: Boolean = false,
    val backupMessage: String? = null,
    val backupError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val importLegacy: ImportLegacyFlightsUseCase,
    private val getSettings: GetSettingsUseCase,
    private val saveSettings: SaveSettingsUseCase,
    private val searchAirports: SearchAirportsUseCase,
    private val exportFlightsCsv: ExportFlightsCsvUseCase,
    private val importFlightsCsv: ImportFlightsCsvUseCase,
    private val exportReferenceJson: ExportReferenceJsonUseCase,
    private val importReferenceJson: ImportReferenceJsonUseCase,
    private val exportLogbookPdfUseCase: ExportLogbookPdfUseCase,
    private val clearAllFlights: ClearAllFlightsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = getSettings()
            _state.update { it.copy(settings = s, homeBaseQuery = s.homeBase) }
        }
    }

    private fun persist(updated: UserSettings) {
        _state.update { it.copy(settings = updated) }
        viewModelScope.launch { saveSettings(updated) }
    }

    fun onPilotNameChange(name: String) = persist(_state.value.settings.copy(pilotName = name))

    fun onDefaultRoleChange(role: PilotRole) = persist(_state.value.settings.copy(defaultRole = role))

    fun onDateFormatChange(format: DateFormat) = persist(_state.value.settings.copy(dateFormat = format))

    fun onHomeBaseQueryChange(query: String) {
        _state.update { it.copy(homeBaseQuery = query) }
        if (query.length >= 2) {
            viewModelScope.launch {
                _state.update { it.copy(homeBaseSuggestions = searchAirports(query)) }
            }
        } else {
            _state.update { it.copy(homeBaseSuggestions = emptyList()) }
        }
    }

    fun onHomeBaseSelected(airport: Airport) {
        _state.update { it.copy(homeBaseQuery = airport.icao, homeBaseSuggestions = emptyList()) }
        persist(_state.value.settings.copy(homeBase = airport.icao))
    }

    // ── Legacy import ───────────────────────────────────────────────────────────

    fun importLegacyJson(context: Context, uri: Uri) {
        if (_state.value.isImporting) return
        _state.update { it.copy(isImporting = true, importResult = null, importError = null) }
        viewModelScope.launch {
            try {
                val result = importLegacy(context, uri)
                _state.update { it.copy(isImporting = false, importResult = result) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isImporting = false, importError = e.message ?: "Import failed")
                }
            }
        }
    }

    fun clearResult() = _state.update { it.copy(importResult = null, importError = null) }

    // ── Backup: flights CSV + reference JSON ──────────────────────────────────────

    private fun runBackup(block: suspend () -> String) {
        if (_state.value.isBackupBusy) return
        _state.update { it.copy(isBackupBusy = true, backupMessage = null, backupError = null) }
        viewModelScope.launch {
            try {
                val message = block()
                _state.update { it.copy(isBackupBusy = false, backupMessage = message) }
            } catch (e: Exception) {
                _state.update { it.copy(isBackupBusy = false, backupError = e.message ?: "Operation failed") }
            }
        }
    }

    fun exportFlights(context: Context, uri: Uri) = runBackup {
        val n = exportFlightsCsv(context, uri)
        "$n flights exported to CSV."
    }

    fun importFlights(context: Context, uri: Uri) = runBackup {
        val n = importFlightsCsv(context, uri)
        "$n flights imported from CSV."
    }

    fun exportReference(context: Context, uri: Uri) = runBackup {
        val c = exportReferenceJson(context, uri)
        buildString {
            append("Exported ${c.airports} custom airports, ${c.types} types, ${c.registrations} registrations")
            if (c.previousTotals) append(", previous totals")
            if (c.settings) append(", settings")
            append(".")
        }
    }

    fun importReference(context: Context, uri: Uri) = runBackup {
        val c = importReferenceJson(context, uri)
        buildString {
            append("Imported ${c.airports} custom airports, ${c.types} types, ${c.registrations} registrations")
            if (c.previousTotals) append(", previous totals")
            if (c.settings) append(", settings")
            append(".")
        }
    }

    fun exportLogbookPdf(context: Context, uri: Uri, fromDate: kotlinx.datetime.LocalDate?) = runBackup {
        val n = exportLogbookPdfUseCase(context, uri, fromDate)
        "Logbook PDF generated ($n flights)" +
            (fromDate?.let { " from $it." } ?: ".")
    }

    fun clearAllFlightsAction() = runBackup {
        clearAllFlights()
        "All flights deleted."
    }

    fun clearBackupResult() = _state.update { it.copy(backupMessage = null, backupError = null) }
}
