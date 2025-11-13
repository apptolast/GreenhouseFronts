package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.data.remote.createHttpClient
import com.apptolast.greenhousefronts.data.remote.websocket.StompWebSocketClient
import com.apptolast.greenhousefronts.data.remote.websocket.createStompClient
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Data layer module containing network and repository dependencies
 */
val dataModule = module {

    // Provide HttpClient as singleton
    single { createHttpClient() }

    // Provide StompClient as singleton
    single { createStompClient() }

    // API Service - depends on HttpClient (injected via constructor)
    singleOf(::GreenhouseApiService)

    // WebSocket Client - depends on StompClient (injected via constructor)
    singleOf(::StompWebSocketClient)

    // Repository Implementation - binds to interface
    // Constructor injection: get() automatically resolves dependencies
    singleOf(::GreenhouseRepositoryImpl) bind GreenhouseRepository::class
}
