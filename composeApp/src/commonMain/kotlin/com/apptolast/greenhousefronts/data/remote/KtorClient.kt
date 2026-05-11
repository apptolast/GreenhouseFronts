package com.apptolast.greenhousefronts.data.remote

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.domain.repository.SessionInvalidator
import com.apptolast.greenhousefronts.util.Environment
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
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
 * HttpClient that attaches the stored access token and silently refreshes on 401 via
 * [SessionInvalidator.tryRefreshOrInvalidate]. On terminal refresh failure the repo
 * invalidates the session and Ktor surfaces the 401 to the caller.
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
        install(Auth) {
            bearer {
                loadTokens {
                    tokenStorage.getToken()?.let { BearerTokens(it, "") }
                }
                // Invoked by Ktor on any 401. Returning null surfaces the 401; returning fresh
                // BearerTokens triggers a transparent retry. Ktor serialises this callback per
                // client; cross-client coalescing (WS path) is handled by AuthRepositoryImpl.
                refreshTokens {
                    println("[AUTH] Ktor refreshTokens fired (401 from upstream)")
                    sessionInvalidator.tryRefreshOrInvalidate()?.let { BearerTokens(it, "") }
                }
                sendWithoutRequest { request -> request.url.host.contains(sendWithoutRequestHostMatch) }
            }
        }
        expectSuccess = true
    }
    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}

val baseUrl: String
    get() = Environment.current.baseUrl
