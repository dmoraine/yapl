// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pilotlog.R

/** Branded launch screen: white background, YAPL logo and the tagline. */
@Composable
fun BrandSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "YAPL",
                modifier = Modifier.width(224.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Yet Another Pilot Log",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D3E50),   // navy, matching the logo
                letterSpacing = 1.5.sp,
            )
        }
    }
}
