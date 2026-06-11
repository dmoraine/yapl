// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.FlightStats
import dev.pilotlog.domain.usecase.statistics.GetStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import javax.inject.Inject

enum class StatsPeriod { ALL_TIME, LAST_90, LAST_12M, LAST_YEAR }

data class StatisticsUiState(
    val period: StatsPeriod = StatsPeriod.ALL_TIME,
    val stats: FlightStats? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStats: GetStatsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsUiState())
    val state: StateFlow<StatisticsUiState> = _state.asStateFlow()

    init { loadStats(StatsPeriod.ALL_TIME) }

    fun selectPeriod(period: StatsPeriod) {
        _state.update { it.copy(period = period, isLoading = true) }
        loadStats(period)
    }

    private fun loadStats(period: StatsPeriod) {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val from = when (period) {
                StatsPeriod.ALL_TIME  -> null
                StatsPeriod.LAST_90   -> today.minus(90, DateTimeUnit.DAY)
                StatsPeriod.LAST_12M  -> today.minus(12, DateTimeUnit.MONTH)
                StatsPeriod.LAST_YEAR -> kotlinx.datetime.LocalDate(today.year, 1, 1)
            }
            val stats = getStats(from = from)
            _state.update { it.copy(stats = stats, isLoading = false) }
        }
    }
}
