package com.apptolast.greenhousefronts.data.remote.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * Singleton object providing configured Krossbow STOMP client
 * Uses Ktor CIO engine with WebSocket support for multiplatform compatibility
 */
object KrossbowClient {

    /**
     * Configured STOMP client instance
     * Uses Ktor WebSocket client for multiplatform support
     */
    val stompClient: StompClient = StompClient(
        KtorWebSocketClient()
    )
}
