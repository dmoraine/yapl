// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PilotLogTypography = Typography(
    // Logbook totals headline
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    // Section titles
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 24.sp,
    ),
    // Flight list row label
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
    ),
    // Secondary info (times, registration)
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
    ),
    // Chip labels, stats subtitles
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
