package com.apptolast.greenhousefronts.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a single data point in a time-series chart.
 *
 * @property timestamp ISO 8601 formatted timestamp (e.g., "2025-01-15T14:30:00Z")
 * @property value The sensor reading value at this timestamp
 */
@Serializable
data class ChartDataPoint(
    val timestamp: String,
    val value: Double
)
