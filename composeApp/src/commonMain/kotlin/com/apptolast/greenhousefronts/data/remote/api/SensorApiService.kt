package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.sensor.HistoricalDataResponse
import com.apptolast.greenhousefronts.data.model.sensor.SensorReadingResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * API service for sensor historical readings and statistics.
 * Uses the authenticated HttpClient.
 */
class SensorApiService(
    private val httpClient: HttpClient,
) {

    /**
     * Gets pre-aggregated historical data with stats from TimescaleDB continuous aggregates.
     * GET /api/v1/statistics/historical-data?code={code}&period={period}
     */
    suspend fun getHistoricalData(code: String, period: String = "24h"): HistoricalDataResponse {
        return httpClient.get("$baseUrl/statistics/historical-data") {
            parameter("code", code)
            parameter("period", period)
        }.body()
    }

    /**
     * Gets raw historical readings for a specific device code.
     * Used for boolean devices that need individual transition points.
     * GET /api/v1/sensors/by-code/{code}?hoursAgo={hoursAgo}
     */
    suspend fun getReadingsByCode(code: String, hoursAgo: Long = 24): List<SensorReadingResponse> {
        return httpClient.get("$baseUrl/sensors/by-code/$code") {
            parameter("hoursAgo", hoursAgo)
        }.body()
    }
}
