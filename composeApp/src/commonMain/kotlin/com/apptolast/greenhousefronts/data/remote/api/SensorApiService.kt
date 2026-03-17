package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.sensor.SensorReadingResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * API service for sensor historical readings.
 * Uses the authenticated HttpClient.
 */
class SensorApiService(
    private val httpClient: HttpClient,
) {

    /**
     * Gets historical readings for a specific device code.
     * GET /api/v1/sensors/by-code/{code}?hoursAgo={hoursAgo}
     */
    suspend fun getReadingsByCode(code: String, hoursAgo: Long = 24): List<SensorReadingResponse> {
        return httpClient.get("$baseUrl/sensors/by-code/$code") {
            parameter("hoursAgo", hoursAgo)
        }.body()
    }
}
