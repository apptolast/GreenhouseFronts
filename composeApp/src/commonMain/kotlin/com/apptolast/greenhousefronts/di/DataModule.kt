package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.local.auth.TokenStorageImpl
import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.data.remote.createAuthenticatedHttpClient
import com.apptolast.greenhousefronts.data.remote.createHttpClient
import com.apptolast.greenhousefronts.data.remote.createUnauthenticatedHttpClient
import com.apptolast.greenhousefronts.data.remote.websocket.StompWebSocketClient
import com.apptolast.greenhousefronts.data.repository.AuthRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

// Named qualifiers for different HttpClient instances
val AUTHENTICATED_CLIENT = named("authenticated")
val UNAUTHENTICATED_CLIENT = named("unauthenticated")

/**
 * Data layer module containing network and repository dependencies
 */
val dataModule = module {

    // JSON configuration
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
            explicitNulls = false
            allowStructuredMapKeys = true
            prettyPrint = false
            useArrayPolymorphism = false
        }
    }

    // Token Storage - uses default platform Settings
    single<TokenStorage> { TokenStorageImpl() }

    // Unauthenticated HttpClient (for login/register)
    single(UNAUTHENTICATED_CLIENT) {
        createUnauthenticatedHttpClient(get())
    }

    // Authenticated HttpClient (for protected endpoints)
    single(AUTHENTICATED_CLIENT) {
        createAuthenticatedHttpClient(get(), get())
    }

    // Legacy HttpClient for backward compatibility with existing code
    single { createHttpClient(get()) }

    // Auth API Service - uses unauthenticated client
    single { AuthApiService(get(UNAUTHENTICATED_CLIENT)) }

    // Auth Repository
    single<AuthRepository> {
        AuthRepositoryImpl(
            authApiService = get(),
            tokenStorage = get()
        )
    }

    // Greenhouse API Service - uses authenticated client
    single { GreenhouseApiService(get(AUTHENTICATED_CLIENT)) }

    // WebSocket Client
    singleOf(::StompWebSocketClient)

    // Greenhouse Repository
    singleOf(::GreenhouseRepositoryImpl) bind GreenhouseRepository::class
}
