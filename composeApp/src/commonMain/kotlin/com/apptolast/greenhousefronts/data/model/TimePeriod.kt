package com.apptolast.greenhousefronts.data.model

/**
 * Enum representing time period filters for historical data.
 *
 * @property apiValue The value used in API requests
 * @property displayName The user-facing name in Spanish
 */
enum class TimePeriod(val apiValue: String, val displayName: String) {
    LAST_24H("24h", "Últimas 24h"),
    LAST_7D("7d", "Últimos 7 días"),
    LAST_30D("30d", "Último Mes");

    companion object {
        /**
         * Finds a TimePeriod by its API value.
         * @param apiValue The API value to search for
         * @return The matching TimePeriod, or null if not found
         */
        fun fromApiValue(apiValue: String): TimePeriod? {
            return entries.find { it.apiValue.equals(apiValue, ignoreCase = true) }
        }
    }
}
