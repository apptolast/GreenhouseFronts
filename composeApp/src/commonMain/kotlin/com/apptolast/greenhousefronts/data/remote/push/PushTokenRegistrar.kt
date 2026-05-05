package com.apptolast.greenhousefronts.data.remote.push

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.remote.api.PushTokenApiService
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the FCM token lifecycle on the client side.
 *
 *  - Reactively observes [AuthRepository.authState]: every transition into
 *    [AuthState.Authenticated] triggers a (re)registration of the device FCM token on the
 *    backend. Idempotent — the backend upserts by token, and `distinctUntilChanged` filters
 *    duplicate transitions where only the bearer changed.
 *  - Subscribes to platform SDK token rotations via [PushTokenProvider.tokenUpdates] so
 *    spontaneous FCM rotations are forwarded to the backend.
 *  - [unregisterCurrentToken]: invoked **before** logout clears the JWT, so the DELETE
 *    request is still authenticated. The reactive collector intentionally does NOT fire
 *    the DELETE on [AuthState.Unauthenticated] because by then the JWT may already be
 *    gone — `AuthViewModel.logout()` orders the unregister call manually before clearing.
 *
 * All network/SDK errors are caught and logged. Push registration is a best-effort
 * background task; nothing in the auth flow should fail because of it.
 *
 * The Koin definition for this class uses `createdAtStart = true` so the constructor runs
 * at app boot and the reactive collectors attach immediately, without needing an explicit
 * call from `App.kt`.
 */
class PushTokenRegistrar(
    private val tokenProvider: PushTokenProvider,
    private val notifier: PushNotifier,
    private val api: PushTokenApiService,
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private var watcherJob: Job? = null
    private var authStateJob: Job? = null

    init {
        startWatchingTokenUpdates()
        startObservingAuthState()
    }

    /**
     * Starts watching for token rotations from the platform SDK. Idempotent —
     * subsequent calls are no-ops while the previous watcher is alive.
     */
    fun startWatchingTokenUpdates() {
        if (watcherJob?.isActive == true) return
        watcherJob = scope.launch {
            try {
                tokenProvider.tokenUpdates().collect { rotated ->
                    sendToBackend(rotated)
                }
            } catch (e: Throwable) {
                println("[FCM] tokenUpdates collection failed: ${e.message}")
            }
        }
    }

    private fun startObservingAuthState() {
        if (authStateJob?.isActive == true) return
        authStateJob = scope.launch {
            // StateFlow already deduplicates by itself (operator fusion) — no need for an
            // explicit `distinctUntilChanged`.
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    registerCurrentToken()
                }
                // Unauthenticated transitions are not handled here — see class kdoc.
            }
        }
    }

    /**
     * Pulls the current platform token (if any) and registers it on the backend. Safe to
     * call repeatedly — the backend upserts by token so duplicate calls are harmless.
     */
    private suspend fun registerCurrentToken() {
        if (tokenStorage.getToken().isNullOrBlank()) {
            println("$TAG no JWT available, skipping registration")
            return
        }

        // Best-effort permission ask. Even on denial we still register: the user can
        // grant the permission later from system settings without re-logging in.
        runCatching { notifier.ensurePermission() }

        val token = runCatching { tokenProvider.currentToken() }.getOrNull()
        if (token.isNullOrBlank()) {
            println("$TAG no FCM token available yet (platform=${tokenProvider.platform})")
            return
        }
        println("$TAG ===== CURRENT FCM TOKEN (platform=${tokenProvider.platform}) =====")
        println("$TAG $token")
        println("$TAG ====================================================")
        sendToBackend(token)
    }

    /**
     * Drops the local token from the backend. Must be called BEFORE the JWT is cleared,
     * because the DELETE endpoint is JWT-protected.
     */
    fun unregisterCurrentToken() {
        scope.launch {
            val token = runCatching { tokenProvider.currentToken() }.getOrNull() ?: return@launch
            runCatching { api.unregister(token) }
                .onFailure { println("[FCM] unregister failed: ${it.message}") }
        }
    }

    private suspend fun sendToBackend(token: String) {
        if (token.isBlank()) return
        runCatching { api.register(token, tokenProvider.platform) }
            .onSuccess { println("$TAG register OK on backend (platform=${tokenProvider.platform})") }
            .onFailure { println("$TAG register failed for platform=${tokenProvider.platform}: ${it.message}") }
    }

    companion object {
        private const val TAG = "[FCM]"
    }
}
