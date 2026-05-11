package com.apptolast.greenhousefronts.data.remote.websocket

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.sendEmptyMsg
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.concurrent.Volatile

/**
 * Thrown when the cached JWT's `exp` is past before we even open the socket. The
 * `retryWhen` block catches this and attempts `/auth/refresh`; on success the upstream
 * resubscribes with the rotated bearer, on failure the repo has already invalidated.
 */
private class TokenExpiredException : RuntimeException("WS bearer expired")

/**
 * Reported as a Crashlytics non-fatal when [tryRefreshOrInvalidate] returns the same bearer
 * that just failed at the WS layer. Distinct exception type so it surfaces as its own row in
 * the Crashlytics dashboard, separate from generic STOMP errors.
 */
private class SameBearerAfterRefreshException : RuntimeException(
    "WS refresh hook returned the same bearer that just failed — likely server-side rejection of an otherwise-fresh JWT",
)

/**
 * Shared STOMP client for the greenhouse status stream.
 *
 * Wire model: the backend pushes a fresh `GreenhouseStatusResponse` to
 * `/user/queue/status/response` on every tenant state change (sensor flush, alert,
 * CRUD). The CONNECT frame carries `Authorization: Bearer <jwt>` so the backend's
 * `StompJwtAuthInterceptor` resolves the principal and targets broadcasts at us.
 *
 * Lifecycle:
 *  - Gated on [AuthRepository.authState] — only Authenticated emissions open a session.
 *  - `distinctUntilChangedBy { it.token }` ⇒ token rotation reopens the connection.
 *  - Pre-check: expired JWT throws [TokenExpiredException] (no point connecting with a
 *    bearer the backend would silently downgrade to anonymous).
 *  - Shared via `shareIn`; the connection lives [STOP_TIMEOUT_MS] past the last subscriber.
 *  - Any other error → reconnect after [RECONNECT_DELAY_MS].
 *
 * SUBSCRIBE→SEND ordering is enforced by the suspending `subscribeText` (returns only
 * after the SUBSCRIBE frame is on the wire); `sendEmptyMsg` then ships on the same TCP
 * connection so Spring's SimpleBroker processes them FIFO. We don't use STOMP RECEIPT
 * frames — SimpleBroker doesn't implement them.
 */
@OptIn(ExperimentalTime::class)
class GreenhouseStatusWebSocket(
    private val authRepository: AuthRepository,
    private val json: Json,
    private val clock: Clock = Clock.System,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Remembers the bearer we last attempted to open with so retryWhen can detect when a
    // refresh handed us the same token that just failed — that's a loop and we should
    // invalidate the session instead of spinning forever.
    @Volatile
    private var lastAttemptedToken: String? = null

    // Cached as singletons — each connect() builds a fresh StompSession but reuses the
    // underlying Ktor HttpClient instead of reallocating one per reconnect.
    private val wsClient by lazy { KtorWebSocketClient() }
    private val stompClient by lazy {
        // 10 s/10 s STOMP heartbeat keeps NAT entries alive on mobile carriers (which
        // drop idle TCP after 30–60 s). Krossbow surfaces missed beats as exceptions →
        // retryWhen reconnects normally. No autoReceipt: SimpleBroker doesn't send them.
        StompClient(wsClient) {
            heartBeat = HeartBeat(minSendPeriod = 10.seconds, expectedPeriod = 10.seconds)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedStatusFlow: SharedFlow<GreenhouseStatusResponse> by lazy {
        authRepository.authState
            .filterIsInstance<AuthState.Authenticated>()
            // Reconnect on token change; ignore expiresAtEpochSec-only diffs.
            .distinctUntilChangedBy { it.token }
            .flatMapLatest { state -> createSessionFlow(state.token) }
            .retryWhen { cause, attempt ->
                if (cause is TokenExpiredException) {
                    // tryRefreshOrInvalidate mutates AuthState itself: on success the new
                    // Authenticated emission re-enters this pipeline with the rotated
                    // token; on failure the session is already invalidated.
                    val previous = lastAttemptedToken
                    val refreshed = authRepository.tryRefreshOrInvalidate()
                    when {
                        refreshed == null -> {
                            log.w { "refresh failed — session invalidated, no retry" }
                            false
                        }

                        refreshed == previous -> {
                            // The refresh path handed us the SAME token that just failed. This
                            // means either the server is rejecting an otherwise-fresh JWT
                            // (signature, kill-switch, principal mismatch) or the coalescing
                            // logic is misfiring. Either way, retrying with the same bearer
                            // would loop — break out and force re-login. Report as a non-fatal
                            // so Crashlytics surfaces it: this is the diagnostic for the bug.
                            log.e(SameBearerAfterRefreshException()) {
                                "refresh returned same bearer that just failed — invalidating session"
                            }
                            authRepository.invalidateSession(AuthState.Reason.EXPIRED)
                            false
                        }

                        else -> {
                            log.i { "token refreshed — reopening WS" }
                            true
                        }
                    }
                } else {
                    log.w(cause) { "error (attempt $attempt), reconnect in ${RECONNECT_DELAY_MS}ms" }
                    delay(RECONNECT_DELAY_MS)
                    true
                }
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)
    }

    /** Hot stream of status snapshots. All collectors share one STOMP session. */
    fun statusFlow(): Flow<GreenhouseStatusResponse> = sharedStatusFlow

    /**
     * Owns one STOMP session for the lifetime of the flow. Order matters:
     *  1. SUBSCRIBE (`subscribeText` suspends until the frame is on the wire).
     *  2. Pump incoming frames via channelFlow.send (back-pressure propagates upstream).
     *  3. SEND the initial request to materialise the first snapshot — broadcasts arrive
     *     unsolicited after that.
     */
    private fun createSessionFlow(token: String): Flow<GreenhouseStatusResponse> = channelFlow {
        lastAttemptedToken = token
        if (JwtDecoder.isTokenExpired(token, clock = clock)) {
            log.i { "bearer expired pre-connect (prefix=${token.take(8)}..) — throwing for refresh" }
            throw TokenExpiredException()
        }

        log.i { "connecting | bearer=${token.take(8)}..(len=${token.length})" }
        val session = connect(token)
        var receiveCount = 0
        var lastReceiveMark: TimeSource.Monotonic.ValueTimeMark? = null

        try {
            val subscription = session.subscribeText(SUBSCRIBE_DESTINATION)
            log.i { "SUBSCRIBED → $SUBSCRIBE_DESTINATION" }

            coroutineScope {
                launch {
                    subscription.collect { msg ->
                        val parsed = json.decodeFromString<GreenhouseStatusResponse>(msg)
                        val now = TimeSource.Monotonic.markNow()
                        val deltaMs = lastReceiveMark?.let { (now - it).inWholeMilliseconds } ?: -1L
                        lastReceiveMark = now
                        receiveCount++
                        // If receiveCount stays at 1 forever, the JWT in CONNECT was rejected
                        // and the STOMP principal is anonymous — broadcasts won't be targeted.
                        log.i { "RECV #$receiveCount Δ${deltaMs}ms ${msg.length}B " + parsed.summarize() }
                        send(parsed)
                    }
                }
                session.sendEmptyMsg(SEND_DESTINATION)
                log.i { "SENT initial → $SEND_DESTINATION" }
            }
        } catch (e: Exception) {
            log.w(e) { "error during STOMP session" }
            throw e
        } finally {
            log.i { "disconnecting (receives=$receiveCount)" }
            runCatching { session.disconnect() }
        }
    }

    private suspend fun connect(token: String): StompSession {
        val wsUrl = Environment.current.wsUrl
        val headers = mapOf("Authorization" to "Bearer $token")
        log.i { "connect $wsUrl bearerLen=${token.length}" }
        return stompClient.connect(wsUrl, customStompConnectHeaders = headers).also {
            log.i { "STOMP CONNECTED" }
        }
    }

    companion object {
        private const val SUBSCRIBE_DESTINATION = "/user/queue/status/response"
        private const val SEND_DESTINATION = "/app/status/request"

        // Tagged Kermit logger — breadcrumbs to Crashlytics on Android/iOS, plain console
        // elsewhere. Use `log.e(throwable)` for non-fatal report-worthy events.
        private val log = Logger.withTag("AUTH-WS")

        /** Keeps the connection alive briefly between screens that share this flow. */
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Cooldown before retrying after a connection error — avoids retry storms. */
        private const val RECONNECT_DELAY_MS = 3_000L
    }
}

/** Mirrors the backend `WsBroadcaster` log format so traces can be correlated by eye. */
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
