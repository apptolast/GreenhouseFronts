package com.apptolast.greenhousefronts.data.remote.websocket

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.util.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
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
 * Maintains a single shared STOMP connection for all subscribers.
 * Multiple screens (GreenhouseDetail, IrrigationConfig, etc.) share the same
 * connection instead of each creating their own, which would cause conflicts
 * on the server's `/user/queue/status/response` channel.
 *
 * - [requestStatus]: Single request-response (for one-shot loads)
 * - [statusFlow]: Shared persistent connection with periodic polling (for real-time screens)
 */
class GreenhouseStatusWebSocket(
    private val tokenStorage: TokenStorage,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Shared flow backed by a single STOMP connection.
     * - Starts connection when the first subscriber appears
     * - Disconnects [STOP_TIMEOUT_MS] after the last subscriber cancels
     * - Automatically reconnects on errors with [RECONNECT_DELAY_MS] delay
     * - replay=1 so new subscribers immediately get the latest status
     */
    private val sharedStatusFlow: SharedFlow<GreenhouseStatusResponse> by lazy {
        createPollFlow()
            .retryWhen { cause, attempt ->
                println("$TAG Connection error (attempt $attempt), reconnecting in ${RECONNECT_DELAY_MS}ms: ${cause::class.simpleName}: ${cause.message}")
                delay(RECONNECT_DELAY_MS)
                true
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)
    }

//    /**
//     * Single request-response. Connects, gets one response, disconnects.
//     */
//    suspend fun requestStatus(): GreenhouseStatusResponse {
//        val session = connect()
//
//        return try {
//            // Channel to bridge the subscription flow to a single receive
//            val responseChannel = Channel<String>(1)
//
//            coroutineScope {
//                // Collect subscription in a separate coroutine
//                val collectJob = launch {
//                    session.subscribeText(SUBSCRIBE_DESTINATION).collect { msg ->
//                        responseChannel.send(msg)
//                    }
//                }
//
//                session.sendEmptyMsg(SEND_DESTINATION)
//                val messageText = responseChannel.receive()
//                collectJob.cancel()
//                json.decodeFromString<GreenhouseStatusResponse>(messageText)
//            }
//        } finally {
//            session.disconnect()
//        }
//    }

    /**
     * Returns a shared Flow that emits GreenhouseStatusResponse periodically.
     * All callers share the same underlying STOMP connection.
     * The connection is opened on first subscriber and closed when no subscribers remain.
     */
    fun statusFlow(): Flow<GreenhouseStatusResponse> = sharedStatusFlow

    /**
     * Creates the raw polling flow that manages the STOMP connection.
     * This is the upstream for [sharedStatusFlow] — only one instance runs at a time.
     */
    private fun createPollFlow(): Flow<GreenhouseStatusResponse> = flow {
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
                        responseChannel.send(parsed)
                    }
                }

                // Polling loop: send request, wait for response, emit
                try {
                    while (true) {
                        session.sendEmptyMsg(SEND_DESTINATION)
                        val response = responseChannel.receive()
                        emit(response)
                        if (POLL_INTERVAL_MS > 0) delay(POLL_INTERVAL_MS)
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

        //        private const val STOP_TIMEOUT_MS = 5_000L
        private const val STOP_TIMEOUT_MS = 0L

        //        private const val RECONNECT_DELAY_MS = 3_000L
        private const val RECONNECT_DELAY_MS = 0L
    }
}
