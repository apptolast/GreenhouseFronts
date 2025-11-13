package com.apptolast.greenhousefronts.data.remote.websocket

import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * Factory function to create a configured Krossbow STOMP client
 * Uses Ktor WebSocket client for multiplatform compatibility
 * Used by Koin for dependency injection
 *
 * @return Configured StompClient instance
 */
fun createStompClient(): StompClient = StompClient(
    KtorWebSocketClient()
)
