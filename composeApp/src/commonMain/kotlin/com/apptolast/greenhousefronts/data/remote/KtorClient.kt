package com.apptolast.greenhousefronts.data.remote

import com.apptolast.greenhousefronts.util.Environment
import io.ktor.client.HttpClient
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
 * Factory function to create a configured HttpClient instance
 * Used by Koin for dependency injection
 *
 * @return Configured HttpClient with Content Negotiation and Logging
 */
fun createHttpClient(jsonConfig: Json) = HttpClient {
    // Content Negotiation configuration for JSON
    install(ContentNegotiation) {
        json(jsonConfig)
    }

    // Logging configuration
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL
    }

    install(WebSockets) {
        pingInterval = 20.toDuration(DurationUnit.SECONDS)
        contentConverter = null
    }

    // Base URL configuration
    expectSuccess = true
}

/**
 * Base URL accessor for environment-based API endpoints
 */
val baseUrl: String
    get() = Environment.current.baseUrl