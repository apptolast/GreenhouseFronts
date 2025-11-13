package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * API service for greenhouse operations
 * Uses constructor injection for HttpClient (provided by Koin)
 *
 * @param httpClient Injected HTTP client for making API requests
 */
class GreenhouseApiService(
    private val httpClient: HttpClient
) {

    /**
     * Retrieves recent greenhouse messages
     * GET /api/greenhouse/messages/recent
     */
    suspend fun getRecentMessages(): List<GreenhouseMessage> {
        return httpClient.get("$baseUrl/api/greenhouse/messages/recent").body()
    }

    /**
     * Publishes a custom message via MQTT
     * POST /api/mqtt/publish/custom
     *
     * @param message The message to publish
     * @param topic MQTT topic destination (default: "GREENHOUSE/RESPONSE")
     * @param qos Quality of Service: 0, 1, or 2 (default: 0)
     */
    suspend fun publishMessage(
        message: GreenhouseMessage,
        topic: String = "GREENHOUSE/RESPONSE",
        qos: Int = 0
    ): String {
        return httpClient.post("$baseUrl/api/mqtt/publish/custom") {
            parameter("topic", topic)
            parameter("qos", qos)
            contentType(ContentType.Application.Json)
            setBody(message)
        }.body()
    }
}