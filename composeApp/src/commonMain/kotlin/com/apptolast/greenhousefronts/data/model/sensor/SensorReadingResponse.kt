package com.apptolast.greenhousefronts.data.model.sensor

import kotlinx.serialization.Serializable

/**
 * Response from GET /api/v1/sensors/by-code/{code}
 */
@Serializable
data class SensorReadingResponse(
    val time: String,
    val code: String,
    val value: String,
)
