package com.apptolast.greenhousefronts.data.remote

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
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
 * @return Configured HttpClient with Auth plugin and Bearer token injection
 */
fun createAuthenticatedHttpClient(
    jsonConfig: Json,
    tokenStorage: TokenStorage
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

            // Send auth headers without waiting for 401
            sendWithoutRequest { request ->
                request.url.host.contains("apptolast.com")
            }
        }
    }

    expectSuccess = true
}

/**
 * Legacy factory function - kept for backward compatibility.
 * Creates an HttpClient without authentication.
 * @deprecated Use createAuthenticatedHttpClient or createUnauthenticatedHttpClient instead.
 */
fun createHttpClient(jsonConfig: Json) = HttpClient {
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

    expectSuccess = true
}

/**
 * Base URL accessor for environment-based API endpoints
 */
val baseUrl: String
    get() = Environment.current.baseUrl