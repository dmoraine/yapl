// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.pilotlog.ui.screen.addedit.AddEditFlightScreen
import dev.pilotlog.ui.screen.detail.FlightDetailScreen
import dev.pilotlog.ui.screen.hangar.HangarScreen
import dev.pilotlog.ui.screen.hangar.TypeRegistrationsScreen
import dev.pilotlog.ui.screen.home.HomeScreen
import dev.pilotlog.ui.screen.logbook.LogbookScreen
import dev.pilotlog.ui.screen.previoustotals.PreviousTotalsScreen
import dev.pilotlog.ui.screen.settings.SettingsScreen
import dev.pilotlog.ui.screen.statistics.StatisticsScreen

@Composable
fun PilotLogNavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues = PaddingValues(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onViewLogbook    = { navController.navigate(Screen.Logbook.route) },
                onAddFlight      = { navController.navigate(Screen.AddFlight.route) },
                onViewLastFlight = { id -> navController.navigate(Screen.FlightDetail.route(id)) },
                onSettings       = { navController.navigate(Screen.Settings.route) },
                contentPadding   = contentPadding,
            )
        }

        composable(Screen.Logbook.route) {
            LogbookScreen(
                onFlightClick  = { id -> navController.navigate(Screen.FlightDetail.route(id)) },
                onAddFlight    = { navController.navigate(Screen.AddFlight.route) },
                contentPadding = contentPadding,
            )
        }

        composable(Screen.Hangar.route) {
            HangarScreen(
                onTypeClick = { code -> navController.navigate(Screen.TypeRegistrations.route(code)) },
                contentPadding = contentPadding,
            )
        }

        composable(
            route = Screen.TypeRegistrations.route,
            arguments = listOf(navArgument("typeCode") { type = NavType.StringType }),
        ) {
            TypeRegistrationsScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(contentPadding = contentPadding)
        }

        composable(
            route = Screen.FlightDetail.route,
            arguments = listOf(navArgument("flightId") { type = NavType.LongType }),
        ) {
            FlightDetailScreen(
                onNavigateUp = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.EditFlight.route(id)) },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateUp = { navController.popBackStack() },
                onPreviousTotals = { navController.navigate(Screen.PreviousTotals.route) },
            )
        }

        composable(Screen.PreviousTotals.route) {
            PreviousTotalsScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AddFlight.route) {
            AddEditFlightScreen(
                onNavigateUp = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.EditFlight.route,
            arguments = listOf(navArgument("flightId") { type = NavType.LongType }),
        ) {
            AddEditFlightScreen(
                onNavigateUp = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
