package com.apptolast.greenhousefronts.data.remote.push

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.remote.api.PushTokenApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the FCM token lifecycle on the client side.
 *
 *  - [registerIfLoggedIn]: invoked at app startup and right after login. Pulls the current
 *    token from the platform SDK and POSTs it to the backend. Skips silently if there is no
 *    JWT yet (registration without auth makes no sense — the backend rejects it).
 *  - The class also subscribes to [PushTokenProvider.tokenUpdates] in its own scope so
 *    spontaneous FCM rotations are forwarded to the backend without any explicit call.
 *  - [unregisterCurrentToken]: invoked **before** logout clears the JWT, so the DELETE
 *    request is still authenticated. After it returns, the caller is free to wipe the
 *    token storage.
 *
 * All network/SDK errors are caught and logged. Push registration is a best-effort
 * background task; nothing in the auth flow should fail because of it.
 */
class PushTokenRegistrar(
    private val tokenProvider: PushTokenProvider,
    private val notifier: PushNotifier,
    private val api: PushTokenApiService,
    private val tokenStorage: TokenStorage,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private var watcherJob: Job? = null

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

    /**
     * Pulls the current token (if any) and registers it on the backend. Safe to call
     * repeatedly — the backend upserts by token so duplicate calls are harmless.
     */
    fun registerIfLoggedIn() {
        scope.launch {
            // No JWT → nothing to do. The next login will retry.
            if (tokenStorage.getToken().isNullOrBlank()) {
                println("$TAG no JWT yet, skipping registration")
                return@launch
            }

            // Best-effort permission ask. Even on denial we still register: the user can
            // grant the permission later from system settings without re-logging in.
            runCatching { notifier.ensurePermission() }

            val token = runCatching { tokenProvider.currentToken() }.getOrNull()
            if (token.isNullOrBlank()) {
                println("$TAG no FCM token available yet (platform=${tokenProvider.platform})")
                return@launch
            }
            println("$TAG ===== CURRENT FCM TOKEN (platform=${tokenProvider.platform}) =====")
            println("$TAG $token")
            println("$TAG ====================================================")
            sendToBackend(token)
        }
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
