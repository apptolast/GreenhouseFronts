package com.apptolast.greenhousefronts.data.model

/**
 * Enum representing the types of sensors that can be monitored.
 *
 * @property apiValue The value used in API requests (uppercase)
 * @property displayName The user-facing name in Spanish
 */
enum class SensorType(val apiValue: String, val displayName: String) {
    TEMPERATURE("TEMPERATURE", "Temperatura"),
    HUMIDITY("HUMIDITY", "Humedad");

    companion object {
        /**
         * Finds a SensorType by its API value.
         * @param apiValue The API value to search for
         * @return The matching SensorType, or null if not found
         */
        fun fromApiValue(apiValue: String): SensorType? {
            return entries.find { it.apiValue.equals(apiValue, ignoreCase = true) }
        }
    }
}
