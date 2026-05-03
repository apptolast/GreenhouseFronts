package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.auth.AuthError
import com.apptolast.greenhousefronts.data.model.auth.ForgotPasswordRequest
import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.LoginRequest
import com.apptolast.greenhousefronts.data.model.auth.RefreshRequest
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.data.model.auth.ResetPasswordRequest
import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.model.SessionEvent
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.util.JwtDecoder
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Source of truth for the session: owns [authState], [sessionEvents], and the refresh /
 * invalidate hooks consumed by Ktor's `bearer { refreshTokens { … } }` block.
 *
 * Backend contract (verified against `inverapi-dev/v3/api-docs`): `/auth/refresh` rotates
 * both tokens; reusing a revoked refresh revokes the whole family. Access TTL ≈ 1h,
 * refresh TTL ≈ 30d.
 */
@OptIn(ExperimentalTime::class)
class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // replay=1 so a Snackbar attached late still sees the last event.
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(replay = 1, extraBufferCapacity = 4)
    override val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    private val sessionMutex = Mutex()

    // Coalesces concurrent refresh attempts (Ktor bearer + WS reconnect can race).
    // Held separately from sessionMutex: persistSuccessfulAuth / invalidateSession take
    // sessionMutex from inside the refresh path, so nested locking would deadlock.
    private val refreshMutex = Mutex()

    override suspend fun bootstrap() {
        // Early-return outside the mutex so nested calls below can't deadlock.
        if (_authState.value !is AuthState.Loading) return

        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) {
            settleUnauthenticated(AuthState.Reason.INITIAL, emitEvent = false)
            return
        }

        if (!JwtDecoder.isTokenExpired(token)) {
            sessionMutex.withLock {
                if (_authState.value !is AuthState.Loading) return
                _authState.value = AuthState.Authenticated(token, JwtDecoder.extractExpiration(token))
            }
            return
        }

        // Access expired but the refresh window may still be open — try it before forcing Login.
        if (refreshTokenLooksUsable()) {
            // tryRefreshOrInvalidate transitions authState itself (Authenticated or
            // Unauthenticated), so no follow-up work is needed regardless of the result.
            tryRefreshOrInvalidate()
            return
        }

        settleUnauthenticated(AuthState.Reason.EXPIRED, emitEvent = true)
    }

    private suspend fun refreshTokenLooksUsable(): Boolean {
        if (tokenStorage.getRefreshToken().isNullOrBlank()) return false
        val refreshExp = tokenStorage.getRefreshExpiry() ?: return true
        // 30 s leeway mirrors JwtDecoder.isTokenExpired — avoids racing the backend clock.
        return refreshExp > Clock.System.now().epochSeconds + 30
    }

    private suspend fun settleUnauthenticated(reason: AuthState.Reason, emitEvent: Boolean) {
        sessionMutex.withLock {
            if (_authState.value !is AuthState.Loading) return
            tokenStorage.clearAll()
            _authState.value = AuthState.Unauthenticated(reason)
            if (emitEvent) {
                _sessionEvents.tryEmit(
                    SessionEvent(
                        message = "Tu sesión ha caducado. Inicia sesión de nuevo.",
                        reason = reason,
                    )
                )
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<JwtResponse> {
        return try {
            val request = LoginRequest(username = email.trim(), password = password)
            val response = authApiService.login(request)

            persistSuccessfulAuth(response)
            Result.success(response)
        } catch (e: ClientRequestException) {
            Result.failure(mapClientError(e))
        } catch (e: Exception) {
            Result.failure(AuthError.NetworkError(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun register(request: RegisterRequest): Result<JwtResponse> {
        return try {
            val response = authApiService.register(request)

            persistSuccessfulAuth(response)
            Result.success(response)
        } catch (e: ClientRequestException) {
            Result.failure(mapClientError(e))
        } catch (e: Exception) {
            Result.failure(AuthError.NetworkError(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val request = ForgotPasswordRequest(email = email)
            authApiService.forgotPassword(request)
            Result.success(Unit)
        } catch (_: ClientRequestException) {
            // Per the backend contract this endpoint returns 200 even when the email is not
            // registered, for security reasons. Treat any 4xx as a quiet success too.
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AuthError.NetworkError(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun resetPassword(token: String, password: String): Result<Unit> {
        return try {
            val request = ResetPasswordRequest(token = token, newPassword = password)
            authApiService.resetPassword(request)
            Result.success(Unit)
        } catch (e: ClientRequestException) {
            Result.failure(mapClientError(e))
        } catch (e: Exception) {
            Result.failure(AuthError.NetworkError(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun logout() {
        try {
            authApiService.logout()
        } catch (_: Exception) {
            // Ignore network errors — always clear local tokens.
        }
        sessionMutex.withLock {
            tokenStorage.clearAll()
            _authState.value = AuthState.Unauthenticated(AuthState.Reason.MANUAL_LOGOUT)
        }
    }

    override suspend fun getToken(): String? = tokenStorage.getToken()

    override suspend fun getUsername(): String? = tokenStorage.getUsername()

    override suspend fun getDisplayName(): String? = tokenStorage.getDisplayName()

    // --- SessionInvalidator ---

    override suspend fun tryRefreshOrInvalidate(): String? = refreshMutex.withLock {
        // Coalesce: if another caller already refreshed inside the mutex, reuse its token.
        val cachedAccess = tokenStorage.getToken()
        if (!cachedAccess.isNullOrBlank() && !JwtDecoder.isTokenExpired(cachedAccess)) {
            return@withLock cachedAccess
        }

        val refresh = tokenStorage.getRefreshToken()
        if (refresh.isNullOrBlank()) {
            invalidateSession(AuthState.Reason.EXPIRED)
            return@withLock null
        }

        try {
            val response = authApiService.refresh(RefreshRequest(refreshToken = refresh))
            persistSuccessfulAuth(response)
            response.token
        } catch (e: ClientRequestException) {
            // 4xx is terminal — 401 means revoked/reused (reuse revokes the family),
            // 400 means malformed. Either way the stored refresh is dead.
            println("[AUTH] /auth/refresh failed HTTP ${e.response.status.value} — invalidating")
            tokenStorage.clearRefreshToken()
            invalidateSession(AuthState.Reason.EXPIRED)
            null
        } catch (e: ServerResponseException) {
            // 5xx incl. 503 kill-switch: keep the refresh (may flip back), invalidate now.
            println("[AUTH] /auth/refresh HTTP ${e.response.status.value} — invalidating, refresh kept")
            invalidateSession(AuthState.Reason.EXPIRED)
            null
        } catch (e: Exception) {
            // Transient I/O — keep both tokens, next caller retries. Null surfaces the
            // original 401 to Ktor as required by the bearer plugin.
            println("[AUTH] /auth/refresh transient failure (${e::class.simpleName}) — kept for retry")
            null
        }
    }

    override suspend fun invalidateSession(reason: AuthState.Reason) {
        sessionMutex.withLock {
            // Idempotent — re-entering with the same reason is a cheap no-op.
            val current = _authState.value
            if (current is AuthState.Unauthenticated && current.reason == reason) return

            tokenStorage.clearAll()
            _authState.value = AuthState.Unauthenticated(reason)
            val message = when (reason) {
                AuthState.Reason.EXPIRED -> "Tu sesión ha caducado. Inicia sesión de nuevo."
                AuthState.Reason.MANUAL_LOGOUT -> "Sesión cerrada."
                AuthState.Reason.INITIAL -> return // no message for cold-start unauth
            }
            _sessionEvents.tryEmit(SessionEvent(message = message, reason = reason))
        }
    }

    // --- Internals ---

    /**
     * Persists the JWT bundle from login / register / refresh and emits Authenticated.
     * If [response] omits `refreshToken` (kill-switch path) the previously stored refresh
     * is kept — wiping it would force a re-login on the next 401.
     */
    private suspend fun persistSuccessfulAuth(response: JwtResponse) {
        sessionMutex.withLock {
            tokenStorage.saveToken(response.token)
            tokenStorage.saveUsername(response.username)
            extractAndSaveJwtClaims(response.token)

            response.refreshToken?.takeIf { it.isNotBlank() }?.let { newRefresh ->
                tokenStorage.saveRefreshToken(newRefresh)
                response.refreshExpiresIn?.let { ttlSec ->
                    val expiresAt = Clock.System.now().epochSeconds + ttlSec
                    tokenStorage.saveRefreshExpiry(expiresAt)
                }
            }

            val exp = JwtDecoder.extractExpiration(response.token)
            _authState.value = AuthState.Authenticated(response.token, exp)
        }
    }

    /** Extracts tenantId and display name from the JWT and persists them. */
    private suspend fun extractAndSaveJwtClaims(token: String) {
        val tenantId = JwtDecoder.extractLongClaim(token, "tenantId")
            ?: JwtDecoder.extractLongClaim(token, "tenant_id")
            ?: JwtDecoder.extractStringClaim(token, "tenantId")?.toLongOrNull()
            ?: JwtDecoder.extractStringClaim(token, "tenant_id")?.toLongOrNull()
        tenantId?.let { tokenStorage.saveTenantId(it) }

        val displayName = JwtDecoder.extractStringClaim(token, "firstName")
            ?: JwtDecoder.extractStringClaim(token, "first_name")
            ?: JwtDecoder.extractStringClaim(token, "name")
        displayName?.let { tokenStorage.saveDisplayName(it) }
    }

    /**
     * Maps Ktor ClientRequestException to specific AuthError types.
     */
    private suspend fun mapClientError(e: ClientRequestException): AuthError {
        return when (e.response.status) {
            HttpStatusCode.Unauthorized -> AuthError.InvalidCredentials()

            HttpStatusCode.BadRequest -> {
                val body = try {
                    e.response.bodyAsText()
                } catch (ex: Exception) {
                    println("Failed to parse error response body: ${ex.message}")
                    "Unable to parse error response"
                }

                when {
                    body.contains("Email already in use", ignoreCase = true) ||
                            body.contains("email", ignoreCase = true) &&
                            body.contains("use", ignoreCase = true) -> {
                        AuthError.EmailAlreadyInUse()
                    }

                    else -> AuthError.ValidationError(
                        message = body.ifBlank { "Datos de registro inválidos" }
                    )
                }
            }

            else -> AuthError.Unknown("Error ${e.response.status.value}: ${e.message}")
        }
    }
}
