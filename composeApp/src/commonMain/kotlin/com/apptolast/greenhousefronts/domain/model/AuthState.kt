package com.apptolast.greenhousefronts.domain.model

/**
 * Single source of truth for the authentication state of the user.
 *
 * Emitted by [com.apptolast.greenhousefronts.domain.repository.AuthRepository.authState] and
 * consumed by:
 *   - the splash route, to decide whether to land on Login or the main graph;
 *   - the global Snackbar listener in App.kt, to surface session-expired feedback;
 *   - the WebSocket singleton, to (re)connect when the bearer changes and disconnect when
 *     the session ends;
 *   - the FCM push registrar, to register/unregister the device token reactively.
 */
sealed interface AuthState {

    /** Bootstrap-in-progress. Splash screen stays visible while we resolve the cached session. */
    data object Loading : AuthState

    /**
     * The user has a valid access token in storage and (best-effort) the `exp` claim is in
     * the future. [expiresAtEpochSec] mirrors the JWT `exp` claim for downstream consumers
     * that want to schedule proactive refresh / disconnect timers; null means the backend
     * issued a token without `exp` (defensive — should not happen in production).
     */
    data class Authenticated(
        val token: String,
        val expiresAtEpochSec: Long?,
    ) : AuthState

    /**
     * No active session. [reason] tells consumers WHY (so the UI can decide whether to show
     * a "session expired" Snackbar, pre-fill the email, or stay quiet on a clean cold boot).
     */
    data class Unauthenticated(val reason: Reason) : AuthState

    enum class Reason {
        /** No token has ever been stored on this device, or the user just opened the app fresh. */
        INITIAL,

        /** Local `exp` check (or backend 401) found the token to be expired. */
        EXPIRED,

        /** The user explicitly tapped "Cerrar sesión". */
        MANUAL_LOGOUT,

        /** Backend rejected an authenticated request and the client could not refresh. */
        INVALIDATED_BY_SERVER,
    }
}

/**
 * One-shot user-facing event emitted alongside an [AuthState] transition. The global
 * Snackbar listener consumes these to show "Tu sesión ha caducado" without duplicating the
 * message on every state collector.
 */
data class SessionEvent(
    val message: String,
    val reason: AuthState.Reason,
)
