// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {

    // Bottom nav destinations
    data object Home       : Screen("home")
    data object Logbook    : Screen("logbook")
    data object Hangar     : Screen("hangar")
    data object Statistics : Screen("statistics")

    // Detail / form screens (not in bottom nav)
    data object FlightDetail : Screen("flight/{flightId}") {
        fun route(id: Long) = "flight/$id"
    }
    data object AddFlight    : Screen("flight/add")
    data object EditFlight   : Screen("flight/{flightId}/edit") {
        fun route(id: Long) = "flight/$id/edit"
    }
    data object Settings        : Screen("settings")
    data object PreviousTotals  : Screen("settings/previous-totals")

    data object TypeRegistrations : Screen("aircraft/type/{typeCode}") {
        fun route(typeCode: String) = "aircraft/type/$typeCode"
    }
}

data class TopLevelRoute(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)

val TOP_LEVEL_ROUTES = listOf(
    TopLevelRoute(Screen.Home,       Icons.Filled.Home,          "Home"),
    TopLevelRoute(Screen.Logbook,    Icons.Filled.Book,          "Logbook"),
    TopLevelRoute(Screen.Hangar,     Icons.Filled.FlightTakeoff, "Hangar"),
    TopLevelRoute(Screen.Statistics, Icons.Filled.BarChart,      "Stats"),
)
