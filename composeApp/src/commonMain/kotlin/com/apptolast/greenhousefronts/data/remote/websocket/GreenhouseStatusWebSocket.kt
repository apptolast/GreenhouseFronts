package com.apptolast.greenhousefronts.data.remote.websocket

import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.util.Environment
import com.apptolast.greenhousefronts.util.JwtDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.sendEmptyMsg
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * Thrown by the WS pipeline when the cached JWT is expired before we even try to connect.
 * Caught by [GreenhouseStatusWebSocket]'s `retryWhen` block, which terminates the upstream
 * (no exponential storm of pointless reconnects) and asks the AuthRepository to invalidate
 * the session — the global Snackbar listener in App.kt then surfaces the error and
 * navigates the user to Login.
 */
private class TokenExpiredException : RuntimeException("WS bearer expired")

/**
 * STOMP WebSocket client for greenhouse status.
 *
 * ## Wire model
 *
 * Pure server-push: the backend (`apptolast/InvernaderosAPI`) emits a fresh
 * `GreenhouseStatusResponse` snapshot via `convertAndSendToUser(email,
 * "/queue/status/response", …)` whenever a tenant's state changes (sensor flush,
 * alert activation/resolution, or any of the CRUD use cases). The STOMP CONNECT
 * frame carries `Authorization: Bearer <jwt>`; `StompJwtAuthInterceptor` on the
 * backend resolves the principal so targeted broadcasts land on this session.
 *
 * The legacy `@MessageMapping("/status/request")` + `@SendToUser` round-trip is
 * still alive on the backend and is used here exactly once per (re)connection
 * (see [createPollFlow] step 3) to materialise the initial snapshot without
 * waiting for the next event.
 *
 * ## Connection lifecycle and AuthState integration
 *
 *  - The upstream is gated on [AuthRepository.authState]: only emissions while the user is
 *    [AuthState.Authenticated] open a STOMP session. A transition to [AuthState.Unauthenticated]
 *    cancels the in-flight connection via `flatMapLatest`, and a token change (different
 *    bearer for the same authenticated user) reopens it transparently.
 *  - Pre-check: before opening, [JwtDecoder.isTokenExpired] inspects the bearer's `exp`
 *    claim. An expired token short-circuits with [TokenExpiredException] and triggers
 *    [AuthRepository.invalidateSession] — there is no point hammering the broker with a
 *    bearer that the backend will silently downgrade to anonymous.
 *  - One shared STOMP connection across all subscribers (see [sharedStatusFlow]).
 *  - The connection opens on the first subscriber and stays alive for [STOP_TIMEOUT_MS]
 *    after the last one cancels — keeps tab-switches between screens cheap.
 *  - On any other error the upstream is restarted after [RECONNECT_DELAY_MS]. `replay = 1`
 *    means a fresh subscriber may still see the previous status while reconnecting.
 *  - SUBSCRIBE/SEND ordering is guaranteed by the synchronous flow in [createPollFlow]:
 *    `subscribeText(...)` is `suspend` and only returns after the SUBSCRIBE frame has been
 *    written to the socket; `sendEmptyMsg(...)` is then called on the same TCP connection,
 *    so the backend's STOMP processor receives both frames in order and registers the
 *    subscription before resolving the request. Auto-receipt (RECEIPT frames) is *not*
 *    used because Spring's SimpleBroker on the backend doesn't implement them.
 */
class GreenhouseStatusWebSocket(
    private val authRepository: AuthRepository,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Krossbow's StompClient/KtorWebSocketClient are stateless wrappers around configuration —
    // a fresh instance per `connect()` allocates a new Ktor HttpClient under the hood every
    // time, which is wasteful in the reconnect loop. Cache both as singletons inside this
    // singleton; each `connect()` still produces an independent StompSession.
    private val wsClient by lazy { KtorWebSocketClient() }
    private val stompClient by lazy {
        // No `autoReceipt`: Spring's SimpleBroker (`enableSimpleBroker(...)` in the
        // backend's WebSocketConfig) does NOT send STOMP RECEIPT frames, so requesting
        // one would always time out with LostReceiptException. We rely instead on the
        // contract documented in `createPollFlow` step (1): `subscribeText()` is
        // `suspend` and only returns once the SUBSCRIBE frame has been written to the
        // socket. Calling `sendEmptyMsg()` afterwards on the same connection guarantees
        // ordered delivery via TCP, and the broker processes frames FIFO per session.
        //
        // `heartBeat = 10s/10s`: STOMP-level keep-alive negotiated with the broker.
        // Mobile carriers and home routers tend to drop idle TCP connections after
        // 30-60 s of inactivity (NAT timeouts), and Spring's SimpleBroker honours STOMP
        // 1.2 heart-beats. With this config the client sends a heart-beat at most every
        // 10 s when nothing else is being written and expects a heart-beat (or any frame)
        // from the server within 10 s. If the negotiated period elapses with no traffic,
        // Krossbow surfaces the disconnection through the channelFlow's exception path
        // and our `retryWhen` reconnects normally.
        StompClient(wsClient) {
            heartBeat = HeartBeat(
                minSendPeriod = 10.seconds,
                expectedPeriod = 10.seconds,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedStatusFlow: SharedFlow<GreenhouseStatusResponse> by lazy {
        authRepository.authState
            .filterIsInstance<AuthState.Authenticated>()
            // Reconnect when the bearer changes (token rotation, login-as-different-user)
            // but skip emissions that change only `expiresAtEpochSec` for the same token.
            .distinctUntilChangedBy { it.token }
            .flatMapLatest { state -> createPollFlow(state.token) }
            .retryWhen { cause, attempt ->
                if (cause is TokenExpiredException) {
                    println("$TAG token expired — invalidating session, no retry")
                    authRepository.invalidateSession(AuthState.Reason.EXPIRED)
                    false
                } else {
                    println("$TAG Connection error (attempt $attempt), reconnecting in ${RECONNECT_DELAY_MS}ms: ${cause::class.simpleName}: ${cause.message}")
                    delay(RECONNECT_DELAY_MS)
                    true
                }
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
    private fun createPollFlow(token: String): Flow<GreenhouseStatusResponse> = channelFlow {
        if (JwtDecoder.isTokenExpired(token)) {
            // No retry, no reconnect — surface upward so retryWhen can route to invalidateSession.
            throw TokenExpiredException()
        }

        println("$TAG statusFlow: connecting...")
        val session = connect(token)
        var receiveCount = 0
        var lastReceiveMark: TimeSource.Monotonic.ValueTimeMark? = null

        try {
            // (1) SUBSCRIBE — suspends until the broker ACKs the SUBSCRIBE frame.
            val subscription = session.subscribeText(SUBSCRIBE_DESTINATION)
            println("$TAG SUBSCRIBED → $SUBSCRIBE_DESTINATION")

            coroutineScope {
                // (2) Pump incoming frames into the channelFlow.
                launch {
                    subscription.collect { msg ->
                        val parsed = json.decodeFromString<GreenhouseStatusResponse>(msg)
                        val now = TimeSource.Monotonic.markNow()
                        val deltaMs = lastReceiveMark?.let { (now - it).inWholeMilliseconds } ?: -1L
                        lastReceiveMark = now
                        receiveCount++
                        // First receive (#1, deltaMs=-1) is the response to /app/status/request below.
                        // Receive #2+ at ~10s cadence confirms the backend's pure-push pipeline
                        // (TenantStatusBroadcastListener → WsBroadcaster.convertAndSendToUser).
                        // If only #1 ever fires, the STOMP principal is anonymous on the backend
                        // and the JWT in CONNECT was rejected — see connect() bearerPresent log.
                        println(
                            "$TAG WS-RECEIVE #$receiveCount deltaMs=$deltaMs sizeChars=${msg.length} " +
                                    parsed.summarize()
                        )
                        send(parsed)
                    }
                }

                // (3) Trigger the first snapshot — safe now that the queue exists.
                session.sendEmptyMsg(SEND_DESTINATION)
                println("$TAG SENT initial → $SEND_DESTINATION (waiting for push frames…)")

                // (4) Diagnostics fallback. Disabled by default (POLL_INTERVAL_MS = 0L)
                // since the backend now pushes fresh snapshots on every tenant state change.
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
            println("$TAG statusFlow: disconnecting (receivesTotal=$receiveCount)")
            runCatching { session.disconnect() }
            println("$TAG statusFlow: disconnected")
        }
    }

    private suspend fun connect(token: String): StompSession {
        val wsUrl = Environment.current.wsUrl

        val headers = mapOf("Authorization" to "Bearer $token")

        // bearerPresent=true is enforced by the upstream `filterIsInstance<Authenticated>`
        // — we never reach this point with a missing token.
        println("$TAG Connecting to $wsUrl bearerPresent=true bearerLen=${token.length}")
        return stompClient.connect(wsUrl, customStompConnectHeaders = headers).also {
            println("$TAG STOMP CONNECTED")
        }
    }

    companion object {
        private const val TAG = "[WS-GREENHOUSE]"
        private const val SUBSCRIBE_DESTINATION = "/user/queue/status/response"
        private const val SEND_DESTINATION = "/app/status/request"

        /**
         * Interval for re-triggering `/app/status/request` as a diagnostics fallback.
         * Default `0L` — the backend has shipped server-side broadcasts
         * (`convertAndSendToUser` on every tenant state change), so the client operates
         * in pure-push mode and the upstream is silent after the initial snapshot.
         * Set a positive value (e.g. `2_000L`) only to debug a suspected push outage.
         */
        private const val POLL_INTERVAL_MS = 0L

        /** Keeps the connection alive briefly between screens that share this flow. */
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Cooldown before retrying after a connection error — avoids retry storms. */
        private const val RECONNECT_DELAY_MS = 3_000L
    }
}

/**
 * One-line counts of the hierarchy delivered in a snapshot. Mirrors the format the
 * backend logs in `WsBroadcaster` (`greenhouses=… sectors=… devices=… settings=… alerts=…`),
 * so client and server logs can be correlated by eye.
 */
private fun GreenhouseStatusResponse.summarize(): String {
    var greenhouses = 0
    var sectors = 0
    var devices = 0
    var settings = 0
    var alerts = 0
    tenants.forEach { t ->
        greenhouses += t.greenhouses.size
        t.greenhouses.forEach { g ->
            sectors += g.sectors.size
            g.sectors.forEach { s ->
                devices += s.devices.size
                settings += s.settings.size
                alerts += s.alerts.size
            }
        }
    }
    return "tenants=${tenants.size} greenhouses=$greenhouses sectors=$sectors " +
            "devices=$devices settings=$settings alerts=$alerts"
}
