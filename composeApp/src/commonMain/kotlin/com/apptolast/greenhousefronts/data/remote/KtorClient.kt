package com.apptolast.greenhousefronts.data.remote

import com.apptolast.greenhousefronts.util.Environment
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory function to create a configured HttpClient instance
 * Used by Koin for dependency injection
 *
 * @return Configured HttpClient with Content Negotiation and Logging
 */
fun createHttpClient() = HttpClient {
    // Content Negotiation configuration for JSON
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Logging configuration
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.BODY
    }

    // Base URL configuration
    expectSuccess = true
}

/**
 * Base URL accessor for environment-based API endpoints
 */
val baseUrl: String
    get() = Environment.current.baseUrl