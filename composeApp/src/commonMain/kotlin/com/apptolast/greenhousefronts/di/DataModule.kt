package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.local.auth.TokenStorageImpl
import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.data.remote.api.UserApiService
import com.apptolast.greenhousefronts.data.remote.createAuthenticatedHttpClient
import com.apptolast.greenhousefronts.data.remote.createUnauthenticatedHttpClient
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import com.apptolast.greenhousefronts.data.repository.AuthRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.UserRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import com.apptolast.greenhousefronts.domain.repository.UserRepository
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

    // Auth API Service - uses unauthenticated client
    single { AuthApiService(get(UNAUTHENTICATED_CLIENT)) }

    // Greenhouse API Service - uses authenticated client
    single { GreenhouseApiService(get(AUTHENTICATED_CLIENT)) }

    // User API Service - uses authenticated client
    single { UserApiService(get(AUTHENTICATED_CLIENT)) }

    // WebSocket service for real-time greenhouse status
    singleOf(::GreenhouseStatusWebSocket)

    // Auth Repository
    single<AuthRepository> {
        AuthRepositoryImpl(
            authApiService = get(),
            tokenStorage = get(),
        )
    }

    // Greenhouse Repository
    singleOf(::GreenhouseRepositoryImpl) bind GreenhouseRepository::class

    // User Repository
    singleOf(::UserRepositoryImpl) bind UserRepository::class
}
