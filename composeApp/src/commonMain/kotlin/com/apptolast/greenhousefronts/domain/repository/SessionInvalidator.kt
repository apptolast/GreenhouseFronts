package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.AuthState

/**
 * Narrow interface implemented by `AuthRepository` and consumed by `KtorClient`'s bearer
 * `refreshTokens` block. Carved out as a separate type so the authenticated `HttpClient`
 * does not need to depend on the full `AuthRepository` (which itself depends on the
 * unauthenticated `HttpClient` via `AuthApiService`) — this is what breaks the otherwise
 * circular Koin graph.
 */
interface SessionInvalidator {

    /**
     * Best-effort refresh: when the backend exposes `/api/v1/auth/refresh`, this will issue
     * the call with the stored refresh token and return the new access token. Until then it
     * is a stub that immediately calls [invalidateSession] with [AuthState.Reason.EXPIRED]
     * and returns `null`, so the Ktor `bearer { refreshTokens { … } }` block treats the
     * 401 as terminal and the caller propagates the failure to the UI.
     *
     * Returning `null` is a load-bearing contract: the Ktor `Auth` plugin interprets a
     * `null` from `refreshTokens` as "give up and surface the original 401".
     */
    suspend fun tryRefreshOrInvalidate(): String?

    /**
     * Marks the current session as gone. Clears persisted credentials, transitions
     * [com.apptolast.greenhousefronts.domain.repository.AuthRepository.authState] to
     * [AuthState.Unauthenticated] with the supplied [reason], and emits a corresponding
     * [com.apptolast.greenhousefronts.domain.model.SessionEvent] for the UI to react to.
     */
    suspend fun invalidateSession(reason: AuthState.Reason)
}
