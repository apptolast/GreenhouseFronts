package com.apptolast.greenhousefronts.data.remote

import co.touchlab.kermit.Logger as KermitLogger
import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.domain.repository.SessionInvalidator
import com.apptolast.greenhousefronts.util.Environment
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/** HttpClient without bearer auth — for `/auth/login`, `/register`, `/refresh`, etc. */
fun createUnauthenticatedHttpClient(
    jsonConfig: Json,
    engine: HttpClientEngine? = null,
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(jsonConfig) }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        expectSuccess = true
    }
    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}

/**
 * Authenticated HttpClient. **Does NOT use Ktor's `bearer { }` plugin** — that plugin
 * caches the result of `loadTokens` internally and only re-reads it on a 401 response.
 * In production we observed two problems with that:
 *
 *   1. After `AuthRepositoryImpl.bootstrap()` rotates the token externally (silent refresh
 *      at cold start), the bearer cache stays glued to the previous token until a 401 is
 *      observed. The WebSocket path reconnects fine (it reads `state.token` directly), but
 *      every REST call keeps sending the old bearer.
 *
 *   2. The backend (Spring Security) returns **HTTP 403** for expired/invalid JWTs, not
 *      401. Ktor's bearer plugin only listens for 401, so the silent-refresh hook never
 *      fires for our backend — meaning the cached stale bearer is never replaced.
 *
 * Instead we install [HttpSend] and:
 *   - On every outgoing request: read `tokenStorage.getToken()` and attach `Authorization:
 *     Bearer …`. No internal cache, so any rotation in storage is picked up immediately.
 *   - On 401 OR 403 response: call [SessionInvalidator.tryRefreshOrInvalidate] once; if it
 *     returns a different bearer, retry the same request with the new one.
 *   - To avoid loops on legitimate 403s we cap the retry at one attempt per request and
 *     bail out if the refresh returned the same bearer we just sent.
 */
fun createAuthenticatedHttpClient(
    jsonConfig: Json,
    tokenStorage: TokenStorage,
    sessionInvalidator: SessionInvalidator,
    engine: HttpClientEngine? = null,
    sendWithoutRequestHostMatch: String = "apptolast.com",
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(jsonConfig) }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        install(WebSockets) {
            pingInterval = 20.toDuration(DurationUnit.SECONDS)
            contentConverter = null
        }
        expectSuccess = true
    }
    val client = if (engine != null) HttpClient(engine, config) else HttpClient(config)
    val log = KermitLogger.withTag("AUTH-HTTP")

    client.plugin(HttpSend).intercept { request ->
        val isProtected = request.url.host.contains(sendWithoutRequestHostMatch) &&
                "auth" !in request.url.pathSegments

        if (isProtected) {
            tokenStorage.getToken()?.let { bearer ->
                request.headers.remove(HttpHeaders.Authorization)
                request.headers.append(HttpHeaders.Authorization, "Bearer $bearer")
            }
        }

        var call = execute(request)
        val status = call.response.status

        // Trigger a refresh on 401 (canonical) OR 403 (this backend's flavour for expired
        // JWTs). Only attempt once: a second 401/403 after a refreshed bearer means the
        // refusal is legitimate (permissions, not auth), and looping would burn refresh
        // tokens.
        if (isProtected && (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden)) {
            val previousAuth = request.headers[HttpHeaders.Authorization]
            log.w {
                "$status from ${request.url.pathSegments.joinToString("/")} — attempting refresh + retry " +
                        "(prevBearer=${previousAuth?.removePrefix("Bearer ")?.take(8)}..)"
            }
            val refreshed = sessionInvalidator.tryRefreshOrInvalidate()
            val newAuth = refreshed?.let { "Bearer $it" }
            if (newAuth != null && newAuth != previousAuth) {
                request.headers.remove(HttpHeaders.Authorization)
                request.headers.append(HttpHeaders.Authorization, newAuth)
                log.i {
                    "retrying ${request.url.pathSegments.joinToString("/")} with refreshed bearer (prefix=${
                        refreshed.take(
                            8
                        )
                    }..)"
                }
                call = execute(request)
            } else if (refreshed == null) {
                log.w { "refresh failed; surfacing $status to caller" }
            } else {
                log.w { "refresh returned identical bearer; not retrying ${request.url.pathSegments.joinToString("/")}" }
            }
        }

        call
    }

    return client
}

val baseUrl: String
    get() = Environment.current.baseUrl
