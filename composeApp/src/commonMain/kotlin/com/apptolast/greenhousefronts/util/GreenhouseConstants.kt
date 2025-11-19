package com.apptolast.greenhousefronts.util

/**
 * Constants for greenhouse identifiers and mappings.
 *
 * Maps local greenhouse IDs (1, 2, 3) to their corresponding UUIDs in the backend.
 * These UUIDs are from the seed data (seed_data_realistic.sql).
 *
 * TODO: Replace this hardcoded mapping with a dynamic endpoint
 *       once GET /api/greenhouses becomes available.
 */
object GreenhouseConstants {

    /**
     * Mapping of local greenhouse IDs to backend UUIDs.
     *
     * Greenhouse 1: Agrícola Sara - Invernadero Sara 01 - Tomates Cherry (SARA_01)
     * Greenhouse 2: Hortalizas Mediterráneo - Invernadero Hortamed A1 - Lechugas Iceberg (HORTAMED_A1)
     * Greenhouse 3: Vivero El Prado - Vivero El Prado V1 - Plantas Ornamentales (ELPRADO_V1)
     */
    val GREENHOUSE_UUID_MAP = mapOf(
        1 to "660e8400-e29b-41d4-a716-446655440001", // Sara 01 - Tomates Cherry
        2 to "660e8400-e29b-41d4-a716-446655440004", // Hortamed A1 - Lechugas Iceberg
        3 to "660e8400-e29b-41d4-a716-446655440006"  // El Prado V1 - Plantas Ornamentales
    )

    /**
     * Gets the backend UUID for a given local greenhouse ID.
     *
     * @param localId The local greenhouse ID (1, 2, or 3)
     * @return The corresponding UUID string
     * @throws IllegalArgumentException if the localId is not valid
     */
    fun getGreenhouseUuid(localId: Int): String {
        return GREENHOUSE_UUID_MAP[localId]
            ?: throw IllegalArgumentException("Invalid greenhouse ID: $localId. Valid IDs are 1, 2, or 3.")
    }

    /**
     * Checks if a local greenhouse ID is valid.
     *
     * @param localId The local greenhouse ID to validate
     * @return true if the ID exists in the mapping, false otherwise
     */
    fun isValidGreenhouseId(localId: Int): Boolean {
        return GREENHOUSE_UUID_MAP.containsKey(localId)
    }
}
