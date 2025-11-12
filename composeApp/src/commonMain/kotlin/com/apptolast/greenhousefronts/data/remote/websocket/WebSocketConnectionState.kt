package com.apptolast.greenhousefronts.data.remote.websocket

/**
 * Represents the state of the WebSocket connection
 *
 * @property isConnected Whether the WebSocket is currently connected
 * @property connectionTime Timestamp when the connection was established (millis since epoch)
 * @property messagesReceived Total number of messages received in this session
 * @property lastError Last error message if connection failed
 * @property reconnectAttempts Number of reconnection attempts made
 */
data class WebSocketConnectionState(
    val isConnected: Boolean = false,
    val connectionTime: Long? = null,
    val messagesReceived: Int = 0,
    val lastError: String? = null,
    val reconnectAttempts: Int = 0
)
