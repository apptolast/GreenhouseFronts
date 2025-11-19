package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.data.remote.createHttpClient
import com.apptolast.greenhousefronts.data.remote.websocket.StompWebSocketClient
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Data layer module containing network and repository dependencies
 */
val dataModule = module {

    single {
        Json {
            // Configuración MUY permisiva para debug
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
            explicitNulls = false
            allowStructuredMapKeys = true
            prettyPrint = false  // Mejor rendimiento
            useArrayPolymorphism = false

            // iOS específico - a veces ayuda
//            classDiscriminator = "type"
        }
    }
    // Provide HttpClient as singleton
    singleOf(::createHttpClient)

    // Provide StompClient as singleton
//    singleOf(::KtorWebSocketClient)

    // API Service - depends on HttpClient (injected via constructor)
    singleOf(::GreenhouseApiService)

    // WebSocket Client - depends on StompClient (injected via constructor)
    singleOf(::StompWebSocketClient)

    // Repository Implementation - binds to interface
    // Constructor injection: get() automatically resolves dependencies
    singleOf(::GreenhouseRepositoryImpl) bind GreenhouseRepository::class
}
