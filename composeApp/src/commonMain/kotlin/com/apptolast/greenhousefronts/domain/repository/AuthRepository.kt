package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.model.SessionEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for authentication operations. Defines the contract for login,
 * registration, session bootstrap and session invalidation.
 *
 * Also extends [SessionInvalidator] so the same implementation can be exposed under both
 * types in DI without an extra adapter — see `DataModule.kt`.
 */
interface AuthRepository : SessionInvalidator {

    /**
     * Reactive view of the current session. Emits [AuthState.Loading] until [bootstrap]
     * runs, then [AuthState.Authenticated] or [AuthState.Unauthenticated]. Consumers use
     * this in place of the legacy [isLoggedIn] check so transitions (login, logout, expiry)
     * propagate automatically.
     */
    val authState: StateFlow<AuthState>

    /**
     * One-shot user-facing events tied to session changes — typically "tu sesión ha
     * caducado" or "tu sesión se ha cerrado". Buffered so a Snackbar consumer that subscribes
     * a few frames after the event is fired still picks it up.
     */
    val sessionEvents: SharedFlow<SessionEvent>

    /**
     * Reads the cached token, validates its `exp`, and emits the appropriate [AuthState].
     * Idempotent — safe to call multiple times. Invoked once by the splash route; further
     * calls are cheap no-ops.
     */
    suspend fun bootstrap()

    /**
     * Authenticates a user with email and password. On success, persists the token, decodes
     * the JWT to extract `exp` / `tenantId` / display-name claims, and emits
     * [AuthState.Authenticated].
     */
    suspend fun login(email: String, password: String): Result<JwtResponse>

    /**
     * Registers a new tenant and admin user. On success, behaves like [login] (auto-login).
     */
    suspend fun register(request: RegisterRequest): Result<JwtResponse>

    /**
     * Requests a password reset email.
     */
    suspend fun forgotPassword(email: String): Result<Unit>

    /**
     * Resets the password using a token received via email.
     */
    suspend fun resetPassword(token: String, password: String): Result<Unit>

    /**
     * Logs out the current user — calls the backend, clears stored credentials and emits
     * [AuthState.Unauthenticated] with reason [AuthState.Reason.MANUAL_LOGOUT].
     */
    suspend fun logout()

    /**
     * Legacy synchronous helper — only checks for token presence, not validity.
     * Kept for backwards compatibility with `AuthViewModel.isLoggedIn()`. Prefer
     * [authState] in new code.
     */
    fun isLoggedIn(): Boolean

    /**
     * Retrieves the stored access token. Returns null if not authenticated.
     */
    suspend fun getToken(): String?

    /**
     * Retrieves the stored username (email).
     */
    suspend fun getUsername(): String?

    /**
     * Retrieves the stored display name for greeting UI.
     */
    suspend fun getDisplayName(): String?
}
