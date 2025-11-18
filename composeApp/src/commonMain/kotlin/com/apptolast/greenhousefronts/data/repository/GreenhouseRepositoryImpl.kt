package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.data.remote.websocket.StompWebSocketClient
import com.apptolast.greenhousefronts.data.remote.websocket.WebSocketConnectionState
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation of GreenhouseRepository
 * Uses constructor injection for dependencies (provided by Koin)
 *
 * @param apiService Injected API service for HTTP operations
 * @param webSocketClient Injected WebSocket client for real-time communication
 */
class GreenhouseRepositoryImpl(
    private val apiService: GreenhouseApiService,
    private val webSocketClient: StompWebSocketClient
) : GreenhouseRepository {

    // HTTP methods
    override suspend fun getRecentMessages(): Result<List<GreenhouseMessage>> {
        return try {
            val messages = apiService.getRecentMessages()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun publishMessage(
        message: GreenhouseMessage,
        topic: String,
        qos: Int
    ): Result<String> {
        return try {
            val response = apiService.publishMessage(message, topic, qos)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatistics(
        greenhouseId: String,
        sensorType: SensorType,
        period: TimePeriod
    ): Result<SensorStatistics> {
        return try {
            val statistics = apiService.getStatistics(
                greenhouseId = greenhouseId,
                sensorType = sensorType.apiValue,
                period = period.apiValue
            )
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // WebSocket methods
    override suspend fun connectWebSocket(): Result<Unit> {
        return webSocketClient.connect()
    }

    override suspend fun observeRealtimeMessages(): Flow<GreenhouseMessage> {
        return webSocketClient.subscribeToMessages()
    }

    override fun getConnectionState(): StateFlow<WebSocketConnectionState> {
        return webSocketClient.connectionState
    }

    override suspend fun disconnectWebSocket() {
        webSocketClient.disconnect()
    }

    override suspend fun reconnectWebSocket(): Result<Unit> {
        return webSocketClient.reconnect()
    }
}