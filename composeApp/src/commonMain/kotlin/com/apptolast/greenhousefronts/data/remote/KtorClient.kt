package com.apptolast.greenhousefronts.data.remote

import com.apptolast.greenhousefronts.util.Environment
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    val httpClient = HttpClient {
        // Configuración de Content Negotiation para JSON
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        // Configuración de Logging
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY
        }

        // Configuración de URL base
        expectSuccess = true
    }

    val baseUrl: String
        get() = Environment.current.baseUrl
}