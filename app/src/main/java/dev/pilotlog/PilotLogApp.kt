// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.pilotlog.data.database.AirportDataRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PilotLogApp : Application() {

    @Inject lateinit var airportDataRefresher: AirportDataRefresher

    override fun onCreate() {
        super.onCreate()
        // Off the main thread: a no-op read of one preference on most launches.
        CoroutineScope(SupervisorJob()).launch { airportDataRefresher.refreshIfNeeded() }
    }
}
