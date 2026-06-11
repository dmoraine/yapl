// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pilotlog.domain.model.FlightStats
import dev.pilotlog.ui.util.toHhMm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
                .fillMaxSize(),
        ) {
            PeriodSelector(
                selected = state.period,
                onSelect = viewModel::selectPeriod,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val stats = state.stats
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                stats == null || stats.totalFlights == 0 -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No flights in this period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> StatsContent(
                    stats = stats,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsPeriod.entries.forEach { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = {
                    Text(
                        when (period) {
                            StatsPeriod.ALL_TIME  -> "All time"
                            StatsPeriod.LAST_90   -> "90 days"
                            StatsPeriod.LAST_12M  -> "12 months"
                            StatsPeriod.LAST_YEAR -> "This year"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }
    }
}

@Composable
private fun StatsContent(stats: FlightStats, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        SectionLabel("Flight hours")
        StatLine("Total block time",    stats.totalMinutes.toHhMm())
        StatLine("Night",               stats.nightMinutes.toHhMm())
        StatLine("IFR",                 stats.ifrMinutes.toHhMm())

        SectionDivider()
        SectionLabel("Role")
        StatLine("PIC",                 stats.picMinutes.toHhMm())
        StatLine("Copilot (SIC)",       stats.copilotMinutes.toHhMm())
        StatLine("Dual",                stats.dualMinutes.toHhMm())
        StatLine("Instructor",          stats.instructorMinutes.toHhMm())

        SectionDivider()
        SectionLabel("Operations")
        StatLine("Total flights",       stats.totalFlights.toString())
        StatLine("Total takeoffs",      stats.totalTakeoffs.toString())
        StatLine("Total landings",      stats.totalLandings.toString())
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
