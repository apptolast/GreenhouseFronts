package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.local.auth.TokenStorageImpl
import com.apptolast.greenhousefronts.data.remote.api.AlertApiService
import com.apptolast.greenhousefronts.data.remote.api.AlertHistoryApiService
import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import com.apptolast.greenhousefronts.data.remote.api.CommandApiService
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.data.remote.api.NotificationLogApiService
import com.apptolast.greenhousefronts.data.remote.api.NotificationPreferencesApiService
import com.apptolast.greenhousefronts.data.remote.api.PushTokenApiService
import com.apptolast.greenhousefronts.data.remote.api.SensorApiService
import com.apptolast.greenhousefronts.data.remote.api.SettingsApiService
import com.apptolast.greenhousefronts.data.remote.api.UserApiService
import com.apptolast.greenhousefronts.data.remote.createAuthenticatedHttpClient
import com.apptolast.greenhousefronts.data.remote.createUnauthenticatedHttpClient
import com.apptolast.greenhousefronts.data.remote.push.PushNotifier
import com.apptolast.greenhousefronts.data.remote.push.PushTokenProvider
import com.apptolast.greenhousefronts.data.remote.push.PushTokenRegistrar
import com.apptolast.greenhousefronts.data.remote.push.providePushNotifier
import com.apptolast.greenhousefronts.data.remote.push.providePushTokenProvider
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import com.apptolast.greenhousefronts.data.repository.AlertRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.AuthRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.NotificationLogRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.NotificationPreferencesRepositoryImpl
import com.apptolast.greenhousefronts.data.repository.UserRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.AlertRepository
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import com.apptolast.greenhousefronts.domain.repository.NotificationLogRepository
import com.apptolast.greenhousefronts.domain.repository.NotificationPreferencesRepository
import com.apptolast.greenhousefronts.domain.repository.SessionInvalidator
import com.apptolast.greenhousefronts.domain.repository.UserRepository
import com.apptolast.greenhousefronts.presentation.navigation.BottomNavSelectionBus
import com.apptolast.greenhousefronts.presentation.navigation.PendingAlertSelectionBus
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

    // Unauthenticated HttpClient (for login/register/forgot/reset/logout)
    single(UNAUTHENTICATED_CLIENT) {
        createUnauthenticatedHttpClient(get())
    }

    // Auth API Service - uses unauthenticated client. Built BEFORE AuthRepository because
    // AuthRepository depends on it.
    single { AuthApiService(get(UNAUTHENTICATED_CLIENT)) }

    // Auth Repository — exposed under both AuthRepository and SessionInvalidator. This is
    // the cycle-breaking trick: the authenticated HttpClient below depends on
    // SessionInvalidator (a tiny interface), not on AuthRepository (which depends on the
    // unauthenticated client). Same singleton, two interfaces.
    single<AuthRepositoryImpl> {
        AuthRepositoryImpl(
            authApiService = get(),
            tokenStorage = get(),
        )
    }
    single<AuthRepository> { get<AuthRepositoryImpl>() }
    single<SessionInvalidator> { get<AuthRepositoryImpl>() }

    // Authenticated HttpClient (for protected endpoints) — depends on SessionInvalidator
    // for the bearer.refreshTokens block.
    single(AUTHENTICATED_CLIENT) {
        createAuthenticatedHttpClient(
            jsonConfig = get(),
            tokenStorage = get(),
            sessionInvalidator = get(),
        )
    }

    // Greenhouse API Service - uses authenticated client
    single { GreenhouseApiService(get(AUTHENTICATED_CLIENT)) }

    // User API Service - uses authenticated client
    single { UserApiService(get(AUTHENTICATED_CLIENT)) }

    // Sensor API Service - uses authenticated client
    single { SensorApiService(get(AUTHENTICATED_CLIENT)) }

    // Settings API Service - uses authenticated client
    single { SettingsApiService(get(AUTHENTICATED_CLIENT)) }

    // Command API Service - sends commands to PLC via MQTT
    single { CommandApiService(get(AUTHENTICATED_CLIENT), get()) }

    // WebSocket service for real-time greenhouse status
    singleOf(::GreenhouseStatusWebSocket)

    // Push notifications: FCM token registration + delivery
    single<PushTokenProvider> { providePushTokenProvider() }
    single<PushNotifier> { providePushNotifier() }
    single { PushTokenApiService(get(AUTHENTICATED_CLIENT)) }
    // createdAtStart = true so the constructor's init block runs at app boot; the
    // registrar then attaches its reactive collectors on AuthState + FCM token rotations
    // without requiring an explicit call from App.kt.
    single(createdAtStart = true) {
        PushTokenRegistrar(
            tokenProvider = get(),
            notifier = get(),
            api = get(),
            tokenStorage = get(),
            authRepository = get(),
        )
    }

    // Greenhouse Repository
    singleOf(::GreenhouseRepositoryImpl) bind GreenhouseRepository::class

    // User Repository
    singleOf(::UserRepositoryImpl) bind UserRepository::class

    // Alerts (REST + repository)
    single { AlertApiService(get(AUTHENTICATED_CLIENT)) }
    single { AlertHistoryApiService(get(AUTHENTICATED_CLIENT)) }
    singleOf(::AlertRepositoryImpl) bind AlertRepository::class

    // Notification preferences (per-user settings)
    single { NotificationPreferencesApiService(get(AUTHENTICATED_CLIENT)) }
    singleOf(::NotificationPreferencesRepositoryImpl) bind NotificationPreferencesRepository::class

    // Notification log (cursor-paginated history of pushes delivered to the user)
    single { NotificationLogApiService(get(AUTHENTICATED_CLIENT)) }
    singleOf(::NotificationLogRepositoryImpl) bind NotificationLogRepository::class

    // Buses for FCM deep-link → Alerts tab routing
    single { PendingAlertSelectionBus() }
    single { BottomNavSelectionBus() }
}
