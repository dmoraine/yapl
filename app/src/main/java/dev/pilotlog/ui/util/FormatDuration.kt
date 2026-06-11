// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.util

fun Int.toHhMm(): String = "%d:%02d".format(this / 60, this % 60)

fun parseHhMm(input: String): Int? {
    val digits = input.filter { it.isDigit() }
    return when (digits.length) {
        1, 2 -> digits.toIntOrNull()?.let { it * 60 }
        3 -> {
            val h = digits[0].digitToInt()
            val m = digits.substring(1).toInt()
            if (m < 60) h * 60 + m else null
        }
        4 -> {
            val h = digits.substring(0, 2).toIntOrNull() ?: return null
            val m = digits.substring(2, 4).toIntOrNull() ?: return null
            if (m < 60) h * 60 + m else null
        }
        else -> null
    }
}
