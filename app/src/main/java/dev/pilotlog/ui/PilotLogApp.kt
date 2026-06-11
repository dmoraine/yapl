// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.pilotlog.ui.navigation.PilotLogNavGraph
import dev.pilotlog.ui.navigation.Screen
import dev.pilotlog.ui.navigation.TOP_LEVEL_ROUTES
import dev.pilotlog.ui.util.LocalDateFormat

// Destinations that show the global "add flight" FAB
private val FAB_DESTINATIONS = setOf(Screen.Home.route, Screen.Logbook.route)

@Composable
fun PilotLogApp(
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val dateFormat by appViewModel.dateFormat.collectAsStateWithLifecycle()

    // The bottom bar belongs only to the top-level tabs. Detail / form / Settings
    // screens navigate via their own back arrow, so the bar is hidden there.
    val isTopLevel = TOP_LEVEL_ROUTES.any { it.screen.route == currentRoute }

    CompositionLocalProvider(LocalDateFormat provides dateFormat) {
    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    TOP_LEVEL_ROUTES.forEach { topLevel ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == topLevel.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(topLevel.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(topLevel.icon, contentDescription = topLevel.label) },
                            label = { Text(topLevel.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute in FAB_DESTINATIONS) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddFlight.route) },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Log flight")
                }
            }
        },
    ) { innerPadding ->
        PilotLogNavGraph(
            navController = navController,
            contentPadding = innerPadding,
        )
    }
    }
}
