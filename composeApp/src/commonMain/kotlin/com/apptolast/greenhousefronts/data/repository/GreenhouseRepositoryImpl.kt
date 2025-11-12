package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.data.remote.websocket.StompWebSocketClient
import com.apptolast.greenhousefronts.data.remote.websocket.WebSocketConnectionState
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class GreenhouseRepositoryImpl(
    private val apiService: GreenhouseApiService = GreenhouseApiService(),
    private val webSocketClient: StompWebSocketClient = StompWebSocketClient()
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