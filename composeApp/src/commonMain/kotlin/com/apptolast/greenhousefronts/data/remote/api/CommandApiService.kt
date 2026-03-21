package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * API service for sending commands to the PLC via the backend.
 * POST /api/v1/commands persists the command and publishes it to MQTT.
 *
 * @param httpClient Injected authenticated HTTP client
 */
class CommandApiService(
    private val httpClient: HttpClient,
) {

    /**
     * Sends a command to the PLC.
     * The backend validates the code, persists to TimescaleDB, and publishes to MQTT.
     *
     * @param code The device or setting code (e.g., "SET-00079", "DEV-00042")
     * @param value The value to send as String
     * @return The persisted command with timestamp
     */
    suspend fun sendCommand(code: String, value: String): DeviceCommandResponse {
        return httpClient.post("$baseUrl/commands") {
            contentType(ContentType.Application.Json)
            setBody(SendCommandRequest(code = code, value = value))
        }.body()
    }

    /**
     * Gets command history for a specific code.
     * GET /api/v1/commands/{code}
     *
     * @param code The device or setting code
     * @return List of past commands ordered by time DESC
     */
    suspend fun getCommandHistory(code: String): List<DeviceCommandResponse> {
        return httpClient.get("$baseUrl/commands/$code").body()
    }
}

@Serializable
data class SendCommandRequest(
    val code: String,
    val value: String,
)

@Serializable
data class DeviceCommandResponse(
    val time: String,
    val code: String,
    val value: String,
)
