package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Multiplatform-safe hex parser. Handles `#RRGGBB`; falls back to gray on malformed input.
 */
internal fun parseHex(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    return runCatching {
        val rgb = cleaned.toLong(16)
        Color(0xFF000000 or rgb)
    }.getOrDefault(Color.Gray)
}

/**
 * `2026-04-30T07:14:23.123Z` → `2026-04-30 07:14`. Round-trips unparseable input unchanged.
 * Converts the parsed instant to the device's current timezone before formatting.
 */
internal fun formatTimestamp(raw: String): String {
    if (raw.isBlank()) return "—"
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.year}-$month-$day $hour:$minute"
}
