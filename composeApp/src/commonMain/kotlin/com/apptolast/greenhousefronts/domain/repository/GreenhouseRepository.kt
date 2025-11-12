package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.remote.websocket.WebSocketConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GreenhouseRepository {
    // HTTP methods
    suspend fun getRecentMessages(): Result<List<GreenhouseMessage>>
    suspend fun publishMessage(
        message: GreenhouseMessage,
        topic: String = "GREENHOUSE/MOBILE",
        qos: Int = 0
    ): Result<String>

    // WebSocket methods
    suspend fun connectWebSocket(): Result<Unit>
    suspend fun observeRealtimeMessages(): Flow<GreenhouseMessage>
    fun getConnectionState(): StateFlow<WebSocketConnectionState>
    suspend fun disconnectWebSocket()
    suspend fun reconnectWebSocket(): Result<Unit>
}