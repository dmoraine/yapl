// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.model.FlightStats
import dev.pilotlog.ui.util.LocalDateFormat
import dev.pilotlog.ui.util.formatted
import dev.pilotlog.ui.util.toHhMm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onViewLogbook: () -> Unit,
    onAddFlight: () -> Unit,
    onViewLastFlight: (Long) -> Unit,
    onSettings: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YAPL") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val allStats = state.allTimeStats
            val last90 = state.last90Stats
            if (allStats != null && allStats.totalFlights > 0) {
                item {
                    StatsCard(
                        title = "All time",
                        stats = allStats,
                        hero = true,
                    )
                }
                if (last90 != null && last90.totalFlights > 0) {
                    item {
                        StatsCard(
                            title = "Last 90 days",
                            stats = last90,
                        )
                    }
                }
                item {
                    Text(
                        "Recent flights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.recentFlights) { flight ->
                    RecentFlightRow(
                        flight = flight,
                        onClick = { onViewLastFlight(flight.id) },
                    )
                }
                item {
                    Button(
                        onClick = onViewLogbook,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("View all flights")
                    }
                }
            } else {
                item { EmptyHome(onAddFlight = onAddFlight) }
            }
        }
    }
}

@Composable
private fun StatsCard(title: String, stats: FlightStats, hero: Boolean = false) {
    val shape = MaterialTheme.shapes.extraLarge
    if (hero) {
        val gradient = Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            StatsContent(title, stats, MaterialTheme.colorScheme.onPrimary, big = true)
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                StatsContent(title, stats, MaterialTheme.colorScheme.onSurface, big = false)
            }
        }
    }
}

@Composable
private fun StatsContent(title: String, stats: FlightStats, contentColor: Color, big: Boolean) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(if (big) 14.dp else 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem("Total", stats.totalMinutes.toHhMm(), contentColor, big)
            StatItem("Night", stats.nightMinutes.toHhMm(), contentColor, big)
            StatItem("IFR", stats.ifrMinutes.toHhMm(), contentColor, big)
            StatItem("Flights", stats.totalFlights.toString(), contentColor, big)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, contentColor: Color, big: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun RecentFlightRow(flight: Flight, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.FlightTakeoff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column {
                Text(
                    "${flight.departureAirport} → ${flight.arrivalAirport}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    flight.date.formatted(LocalDateFormat.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            flight.totalMinutes.toHhMm(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyHome(onAddFlight: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Filled.FlightTakeoff,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Welcome to YAPL",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Log your first flight to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAddFlight) { Text("Log a flight") }
    }
}
