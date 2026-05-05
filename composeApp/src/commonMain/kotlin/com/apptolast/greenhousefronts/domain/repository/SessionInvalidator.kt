package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.AuthState

/**
 * Narrow refresh-and-invalidate slice of [AuthRepository]. Exposed separately so the
 * authenticated `HttpClient` (which the repo depends on transitively) does not have to
 * depend on the full repo — breaks the otherwise circular Koin graph.
 */
interface SessionInvalidator {

    /**
     * `POST /auth/refresh` with the stored refresh token. Returns the new access token or
     * `null` if the session could not be recovered. Returning `null` is load-bearing —
     * the Ktor `Auth` plugin treats it as "give up and surface the 401".
     *
     *  - 200          → persists the rotated pair, sets Authenticated, returns new access.
     *  - 4xx          → drops the dead refresh + invalidates the session.
     *  - 5xx (incl. 503 kill-switch) → keeps the refresh, invalidates the session.
     *  - I/O failure  → keeps both tokens, no invalidation; next caller retries.
     *
     * Concurrent callers are coalesced internally — only the first issues the HTTP call.
     */
    suspend fun tryRefreshOrInvalidate(): String?

    /**
     * Clears credentials, transitions [AuthRepository.authState] to Unauthenticated, and
     * emits a [com.apptolast.greenhousefronts.domain.model.SessionEvent] with [reason].
     */
    suspend fun invalidateSession(reason: AuthState.Reason)
}
