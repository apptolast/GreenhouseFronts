package com.apptolast.greenhousefronts.data.remote.websocket

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.util.Environment
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendEmptyMsg
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * STOMP WebSocket client for greenhouse status.
 *
 * ## Wire model
 *
 * The backend (`apptolast/InvernaderosAPI`) currently exposes a single
 * `@MessageMapping("/status/request")` controller that returns the full hierarchy via
 * `@SendToUser("/queue/status/response")`. There is **no** server-initiated broadcast
 * yet, so the client has to ping `/app/status/request` periodically to stay reasonably
 * fresh (every [POLL_INTERVAL_MS]).
 *
 * When the backend adds `SimpMessagingTemplate.convertAndSendToUser(...)` on data-change
 * events (MQTT inbound, alert activation, command confirmation), set
 * [POLL_INTERVAL_MS] to `0L` to make this client a pure server-push consumer — no other
 * change is required, the subscribe/dispatch pipeline already handles incoming frames
 * the same way.
 *
 * ## Connection lifecycle
 *
 *  - One shared STOMP connection across all subscribers (see [sharedStatusFlow]).
 *  - The connection opens on the first subscriber and stays alive for [STOP_TIMEOUT_MS]
 *    after the last one cancels — keeps tab-switches between screens cheap.
 *  - On error the upstream is restarted after [RECONNECT_DELAY_MS]. `replay = 1` means a
 *    fresh subscriber may still see the previous status while the reconnection is in flight.
 *  - `autoReceipt = true` on the underlying client makes both `subscribeText(...)` and
 *    `sendEmptyMsg(...)` suspend until the broker ACKs the frame, eliminating the
 *    SUBSCRIBE/SEND race that previously left the UI stuck on Loading whenever the
 *    network reordered our two opening frames.
 */
class GreenhouseStatusWebSocket(
    private val tokenStorage: TokenStorage,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sharedStatusFlow: SharedFlow<GreenhouseStatusResponse> by lazy {
        createPollFlow()
            .retryWhen { cause, attempt ->
                println("$TAG Connection error (attempt $attempt), reconnecting in ${RECONNECT_DELAY_MS}ms: ${cause::class.simpleName}: ${cause.message}")
                delay(RECONNECT_DELAY_MS)
                true
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)
    }

    /**
     * Hot, shared status stream. Multiple ViewModels can collect this Flow simultaneously
     * — they all share a single underlying STOMP connection. New subscribers immediately
     * receive the last cached status (replay = 1).
     */
    fun statusFlow(): Flow<GreenhouseStatusResponse> = sharedStatusFlow

    /**
     * Builds the upstream pipeline that owns the STOMP connection.
     *
     * Order of operations is critical and **must not** be reordered:
     *  1. SUBSCRIBE first — with auto-receipt, the call only returns after the broker
     *     confirms the subscription, so the server-side queue exists before any SEND.
     *  2. Forward incoming frames asynchronously — `channelFlow.send` propagates
     *     back-pressure naturally to the subscription.
     *  3. SEND the initial request to materialise the first snapshot.
     *  4. Optionally keep refreshing while [POLL_INTERVAL_MS] is positive. When the
     *     backend gains broadcast support, this loop is disabled (interval = 0) and the
     *     flow degenerates into pure push consumption — no other code path changes.
     */
    private fun createPollFlow(): Flow<GreenhouseStatusResponse> = channelFlow {
        println("$TAG statusFlow: connecting...")
        val session = connect()

        try {
            // (1) SUBSCRIBE — suspends until the broker ACKs the SUBSCRIBE frame.
            val subscription = session.subscribeText(SUBSCRIBE_DESTINATION)

            coroutineScope {
                // (2) Pump incoming frames into the channelFlow.
                launch {
                    subscription.collect { msg ->
                        val parsed = json.decodeFromString<GreenhouseStatusResponse>(msg)
                        send(parsed)
                    }
                }

                // (3) Trigger the first snapshot — safe now that the queue exists.
                session.sendEmptyMsg(SEND_DESTINATION)

                // (4) Periodic refresh (temporary while the backend has no broadcast).
                if (POLL_INTERVAL_MS > 0L) {
                    while (isActive) {
                        delay(POLL_INTERVAL_MS)
                        session.sendEmptyMsg(SEND_DESTINATION)
                    }
                }
            }
        } catch (e: Exception) {
            println("$TAG statusFlow error: ${e::class.simpleName}: ${e.message}")
            throw e
        } finally {
            println("$TAG statusFlow: disconnecting...")
            runCatching { session.disconnect() }
            println("$TAG statusFlow: disconnected")
        }
    }

    private suspend fun connect(): StompSession {
        val token = tokenStorage.getToken()
        val wsUrl = Environment.current.wsUrl

        val wsClient = KtorWebSocketClient()
        // Auto-receipt makes SUBSCRIBE/SEND suspend until the broker ACKs the frame.
        // It is the only robust way to guarantee the SUBSCRIBE arrives before our first
        // SEND in createPollFlow — without it, the broker may discard the response when
        // the SEND is processed before the SUBSCRIBE.
        val stompClient = StompClient(wsClient) {
            autoReceipt = true
            receiptTimeout = 5.seconds
        }

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

        /**
         * How often to re-trigger `/app/status/request` while the backend lacks
         * server-initiated broadcasts. Set to `0L` once the backend adds
         * `convertAndSendToUser(...)` on data-change events to disable polling entirely.
         */
        private const val POLL_INTERVAL_MS = 2_000L

        /** Keeps the connection alive briefly between screens that share this flow. */
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Cooldown before retrying after a connection error — avoids retry storms. */
        private const val RECONNECT_DELAY_MS = 3_000L
    }
}
