package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.model.SessionEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the user's session. Also exposed as [SessionInvalidator] in
 * DI so the Ktor `bearer { refreshTokens { … } }` block can reach refresh / invalidate
 * without depending on the full repo (cycle-breaking — see `DataModule.kt`).
 */
interface AuthRepository : SessionInvalidator {

    /** Loading until [bootstrap] runs, then Authenticated / Unauthenticated. */
    val authState: StateFlow<AuthState>

    /**
     * One-shot session-change messages for the global Snackbar. Buffered (replay = 1) so a
     * subscriber that arrives a few frames late still picks the event up.
     */
    val sessionEvents: SharedFlow<SessionEvent>

    /**
     * Resolves the cached session and settles [authState]. Called once by the splash;
     * idempotent — further calls are cheap no-ops.
     */
    suspend fun bootstrap()

    suspend fun login(email: String, password: String): Result<JwtResponse>
    suspend fun register(request: RegisterRequest): Result<JwtResponse>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun resetPassword(token: String, password: String): Result<Unit>

    /** Calls `/auth/logout` (best-effort), clears local state, emits MANUAL_LOGOUT. */
    suspend fun logout()

    suspend fun getToken(): String?
    suspend fun getUsername(): String?
    suspend fun getDisplayName(): String?
}
