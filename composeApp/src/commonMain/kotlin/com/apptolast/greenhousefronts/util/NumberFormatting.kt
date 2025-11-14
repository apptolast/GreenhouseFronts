package com.apptolast.greenhousefronts.util

import kotlin.math.pow
import kotlin.math.round

/**
 * Formats a Double to a string with the specified number of decimal places.
 *
 * This is a multiplatform-compatible alternative to String.format(),
 * which is only available on JVM platforms.
 *
 * @param decimals Number of decimal places to show (0 for integers)
 * @return Formatted string representation of the number
 *
 * Examples:
 * - 23.456.formatDecimals(1) = "23.5"
 * - 65.7.formatDecimals(0) = "66"
 * - 100.0.formatDecimals(2) = "100.0"
 */
fun Double.formatDecimals(decimals: Int): String {
    require(decimals >= 0) { "Number of decimals must be non-negative" }

    if (decimals == 0) {
        // For zero decimals, round to integer
        return round(this).toInt().toString()
    }

    // Round to specified decimal places
    val multiplier = 10.0.pow(decimals)
    val rounded = round(this * multiplier) / multiplier

    // Format the number ensuring the correct number of decimal places
    val formatted = rounded.toString()
    val parts = formatted.split('.')

    return if (parts.size == 1) {
        // No decimal point, add zeros
        "$formatted.${"0".repeat(decimals)}"
    } else {
        // Has decimal point, pad if needed
        val currentDecimals = parts[1].length
        if (currentDecimals < decimals) {
            "$formatted${"0".repeat(decimals - currentDecimals)}"
        } else {
            formatted
        }
    }
}

/**
 * Formats a Float to a string with the specified number of decimal places.
 *
 * Delegates to the Double extension function.
 */
fun Float.formatDecimals(decimals: Int): String = this.toDouble().formatDecimals(decimals)