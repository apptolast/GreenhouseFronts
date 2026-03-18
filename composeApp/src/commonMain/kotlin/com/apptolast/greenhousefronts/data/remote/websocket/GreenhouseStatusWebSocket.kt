package com.apptolast.greenhousefronts.data.remote.websocket

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.util.Environment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendEmptyMsg
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * STOMP WebSocket client for greenhouse status.
 *
 * - [requestStatus]: Single request-response (for one-shot loads)
 * - [statusFlow]: Persistent connection with periodic polling (for real-time screens)
 */
class GreenhouseStatusWebSocket(
    private val tokenStorage: TokenStorage,
    private val json: Json,
) {

    /**
     * Single request-response. Connects, gets one response, disconnects.
     */
    suspend fun requestStatus(): GreenhouseStatusResponse {
        val session = connect()

        return try {
            // Channel to bridge the subscription flow to a single receive
            val responseChannel = Channel<String>(1)

            coroutineScope {
                // Collect subscription in a separate coroutine
                val collectJob = launch {
                    session.subscribeText(SUBSCRIBE_DESTINATION).collect { msg ->
                        responseChannel.send(msg)
                    }
                }

                session.sendEmptyMsg(SEND_DESTINATION)
                val messageText = responseChannel.receive()
                collectJob.cancel()
                json.decodeFromString<GreenhouseStatusResponse>(messageText)
            }
        } finally {
            session.disconnect()
        }
    }

    /**
     * Returns a Flow that emits GreenhouseStatusResponse periodically.
     * Keeps the STOMP session open, sends a request every [intervalMs] ms.
     * The connection is closed when the Flow collector is cancelled.
     */
    fun statusFlow(intervalMs: Long = POLL_INTERVAL_MS): Flow<GreenhouseStatusResponse> = flow {
        println("$TAG statusFlow: connecting...")
        val session = connect()

        try {
            // Channel to receive parsed responses from the subscription coroutine
            val responseChannel = Channel<GreenhouseStatusResponse>(Channel.CONFLATED)

            coroutineScope {
                // Coroutine that collects all messages from the subscription
                val collectJob = launch {
                    session.subscribeText(SUBSCRIBE_DESTINATION).collect { msg ->
                        val parsed = json.decodeFromString<GreenhouseStatusResponse>(msg)
                        println("$TAG statusFlow: received ${parsed.tenants.size} tenants, ts=${parsed.timestamp}")
                        responseChannel.send(parsed)
                    }
                }

                // Polling loop: send request, wait for response, emit, delay
                try {
                    while (true) {
                        println("$TAG statusFlow: requesting update...")
                        session.sendEmptyMsg(SEND_DESTINATION)
                        val response = responseChannel.receive()
                        emit(response)
                        if (intervalMs > 0) delay(intervalMs)
                    }
                } finally {
                    collectJob.cancel()
                }
            }
        } catch (e: Exception) {
            println("$TAG statusFlow error: ${e::class.simpleName}: ${e.message}")
            throw e
        } finally {
            println("$TAG statusFlow: disconnecting...")
            try {
                session.disconnect()
            } catch (_: Exception) {
            }
            println("$TAG statusFlow: disconnected")
        }
    }

    private suspend fun connect(): StompSession {
        val token = tokenStorage.getToken()
        val wsUrl = Environment.current.wsUrl

        val wsClient = KtorWebSocketClient()
        val stompClient = StompClient(wsClient)

        val headers = buildMap {
            if (token != null) {
                put("Authorization", "Bearer $token")
            }
        }

        println("$TAG Connecting to $wsUrl...")
        return stompClient.connect(wsUrl, customStompConnectHeaders = headers).also {
            println("$TAG STOMP CONNECTED")
        }
    }

    companion object {
        private const val TAG = "[WS-GREENHOUSE]"
        private const val SUBSCRIBE_DESTINATION = "/user/queue/status/response"
        private const val SEND_DESTINATION = "/app/status/request"
        private const val POLL_INTERVAL_MS = 0L
    }
}
