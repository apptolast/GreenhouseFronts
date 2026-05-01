package com.apptolast.greenhousefronts.data.remote

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.domain.repository.SessionInvalidator
import com.apptolast.greenhousefronts.util.Environment
import io.ktor.client.HttpClient
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

/**
 * Factory function to create an UNAUTHENTICATED HttpClient instance.
 * Used for login/register endpoints that don't require Bearer tokens.
 *
 * @param jsonConfig JSON serialization configuration
 * @return Configured HttpClient without Auth plugin
 */
fun createUnauthenticatedHttpClient(jsonConfig: Json) = HttpClient {
    install(ContentNegotiation) {
        json(jsonConfig)
    }

    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL
    }

    expectSuccess = true
}

/**
 * Factory function to create an AUTHENTICATED HttpClient instance.
 * Automatically injects Bearer token from TokenStorage into requests.
 * Used for protected API endpoints.
 *
 * @param jsonConfig JSON serialization configuration
 * @param tokenStorage Token storage for retrieving the current access token
 * @param sessionInvalidator Hook called by Ktor on a 401 — today it forwards to the
 *   AuthRepository to clear the session and emit Unauthenticated; later (when the backend
 *   ships `/auth/refresh`) it will return a refreshed access token transparently.
 * @return Configured HttpClient with Auth plugin and Bearer token injection
 */
fun createAuthenticatedHttpClient(
    jsonConfig: Json,
    tokenStorage: TokenStorage,
    sessionInvalidator: SessionInvalidator,
) = HttpClient {
    install(ContentNegotiation) {
        json(jsonConfig)
    }

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
                val token = tokenStorage.getToken()
                if (token != null) {
                    BearerTokens(token, "")
                } else {
                    null
                }
            }

            // Invoked automatically by Ktor on any 401 response. Per the official Ktor 3.x
            // bearer-auth docs (https://ktor.io/docs/client-bearer-auth.html), returning
            // null here makes Ktor surface the 401 to the caller; returning a fresh
            // BearerTokens triggers a transparent retry of the original request.
            //
            // Today the backend has no refresh endpoint, so `tryRefreshOrInvalidate()` is a
            // stub that clears the session and returns null. When `/auth/refresh` ships,
            // the implementation is replaced and this block requires no changes.
            refreshTokens {
                val newToken = sessionInvalidator.tryRefreshOrInvalidate()
                newToken?.let { BearerTokens(it, "") }
            }

            // Send auth headers without waiting for 401
            sendWithoutRequest { request ->
                request.url.host.contains("apptolast.com")
            }
        }
    }

    expectSuccess = true
}

/**
 * Base URL accessor for environment-based API endpoints
 */
val baseUrl: String
    get() = Environment.current.baseUrl