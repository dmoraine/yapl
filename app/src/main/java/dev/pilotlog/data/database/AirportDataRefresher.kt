// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.data.database

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-seeds the airport reference table from the bundled asset.
 *
 * `createFromAsset` only runs when the database is first created, so airports fixed
 * in a later release (renames, new ICAO/IATA assignments) would never reach anyone
 * who already had the app installed. This replays the asset whenever its data
 * version moves ahead of what the install last applied.
 *
 * User-created or user-edited airports (`is_custom = 1`) are left untouched: they are
 * kept out of the delete and then block the asset row for the same ICAO on insert.
 */
@Singleton
class AirportDataRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PilotLogDatabase,
) {

    /** Replay the bundled airports if this install has not applied them yet. */
    suspend fun refreshIfNeeded() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_DATA_VERSION, 0) >= ASSET_DATA_VERSION) return@withContext
        runCatching { replayAsset() }
            .onSuccess {
                prefs.edit().putInt(KEY_DATA_VERSION, ASSET_DATA_VERSION).apply()
                Log.i(TAG, "Airport reference data refreshed to v$ASSET_DATA_VERSION")
            }
            // Reference data is not worth crashing over — keep whatever is already there
            // and try again on the next launch.
            .onFailure { Log.w(TAG, "Airport refresh failed, keeping existing data", it) }
    }

    private fun replayAsset() {
        // ATTACH needs a real file, and assets are not one.
        val assetCopy = File(context.cacheDir, "airports-ref.db")
        context.assets.open(ASSET_NAME).use { input ->
            assetCopy.outputStream().use { output -> input.copyTo(output) }
        }
        try {
            val db = database.openHelper.writableDatabase
            // ATTACH is rejected inside a transaction, so it stays outside the one below.
            db.execSQL("ATTACH DATABASE ? AS ref", arrayOf(assetCopy.absolutePath))
            try {
                db.beginTransaction()
                try {
                    db.execSQL("DELETE FROM airports WHERE is_custom = 0")
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO airports
                            (icao, iata, name, municipality, country,
                             latitude, longitude, elevation_ft, timezone, is_custom)
                        SELECT icao, iata, name, municipality, country,
                               latitude, longitude, elevation_ft, timezone, 0
                        FROM ref.airports
                        """,
                    )
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } finally {
                db.execSQL("DETACH DATABASE ref")
            }
        } finally {
            assetCopy.delete()
        }
    }

    companion object {
        /**
         * Bump this whenever `app/src/main/assets/airports.db` is regenerated, otherwise
         * existing installs keep their current airport table.
         */
        const val ASSET_DATA_VERSION = 2

        private const val ASSET_NAME = "airports.db"
        private const val PREFS_NAME = "airport_data"
        private const val KEY_DATA_VERSION = "asset_data_version"
        private const val TAG = "AirportDataRefresher"
    }
}
