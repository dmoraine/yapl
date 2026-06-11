// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.pilotlog.ui.BrandSplash
import dev.pilotlog.ui.PilotLogApp
import dev.pilotlog.ui.theme.PilotLogTheme
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PilotLogTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1300)
                    showSplash = false
                }
                Box(Modifier.fillMaxSize()) {
                    PilotLogApp()
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(animationSpec = tween(durationMillis = 450)),
                    ) {
                        BrandSplash()
                    }
                }
            }
        }
    }
}
