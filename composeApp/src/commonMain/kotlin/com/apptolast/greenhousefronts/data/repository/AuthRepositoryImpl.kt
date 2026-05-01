package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.auth.AuthError
import com.apptolast.greenhousefronts.data.model.auth.ForgotPasswordRequest
import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.LoginRequest
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.data.model.auth.ResetPasswordRequest
import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.model.SessionEvent
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.util.JwtDecoder
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of [AuthRepository].
 *
 * In addition to the legacy login/register/logout flow, this class is now the single
 * source of truth for the session: it owns the [authState] StateFlow, the [sessionEvents]
 * SharedFlow, and the [invalidateSession] / [tryRefreshOrInvalidate] entry points consumed
 * by the Ktor `bearer { refreshTokens { … } }` block in `KtorClient.kt`.
 *
 * Backend constraint (verified on inverapi-dev Swagger): there is no `/auth/refresh`
 * endpoint yet. [tryRefreshOrInvalidate] is therefore a stub that immediately invalidates
 * the session and returns `null`. When the backend ships the endpoint, the body of that
 * method becomes the actual refresh call — no caller changes.
 */
class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Replay = 1 so a Snackbar that subscribes a few frames after `invalidateSession` was
    // called still picks up the message. Buffered to avoid blocking emitters.
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(replay = 1, extraBufferCapacity = 4)
    override val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    // Guards against concurrent bootstrap / invalidate races.
    private val sessionMutex = Mutex()

    override suspend fun bootstrap() {
        sessionMutex.withLock {
            // If we've already settled to Authenticated/Unauthenticated for THIS app session,
            // a second bootstrap call is a no-op. We only re-evaluate while still Loading.
            if (_authState.value !is AuthState.Loading) return

            val token = tokenStorage.getToken()
            if (token.isNullOrBlank()) {
                _authState.value = AuthState.Unauthenticated(AuthState.Reason.INITIAL)
                return
            }

            if (JwtDecoder.isTokenExpired(token)) {
                tokenStorage.clearAll()
                _authState.value = AuthState.Unauthenticated(AuthState.Reason.EXPIRED)
                _sessionEvents.tryEmit(
                    SessionEvent(
                        message = "Tu sesión ha caducado. Inicia sesión de nuevo.",
                        reason = AuthState.Reason.EXPIRED,
                    )
                )
                return
            }

            val exp = JwtDecoder.extractExpiration(token)
            if (exp != null) tokenStorage.saveTokenExpiry(exp)
            _authState.value = AuthState.Authenticated(token, exp)
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

    override fun isLoggedIn(): Boolean = tokenStorage.hasToken()

    override suspend fun getToken(): String? = tokenStorage.getToken()

    override suspend fun getUsername(): String? = tokenStorage.getUsername()

    override suspend fun getDisplayName(): String? = tokenStorage.getDisplayName()

    // --- SessionInvalidator ---

    override suspend fun tryRefreshOrInvalidate(): String? {
        // Forward-compat hook. Today: the backend has no /auth/refresh, so any 401 is
        // terminal — clear the session and surface the EXPIRED reason. When backend ships
        // the endpoint, replace this body with the actual call (using
        // `markAsRefreshTokenRequest()` on the request to avoid recursion).
        invalidateSession(AuthState.Reason.EXPIRED)
        return null
    }

    override suspend fun invalidateSession(reason: AuthState.Reason) {
        sessionMutex.withLock {
            // Idempotent — re-entering with the same reason is a cheap no-op.
            val current = _authState.value
            if (current is AuthState.Unauthenticated && current.reason == reason) return

            tokenStorage.clearAll()
            _authState.value = AuthState.Unauthenticated(reason)
            val message = when (reason) {
                AuthState.Reason.EXPIRED,
                AuthState.Reason.INVALIDATED_BY_SERVER ->
                    "Tu sesión ha caducado. Inicia sesión de nuevo."

                AuthState.Reason.MANUAL_LOGOUT -> "Sesión cerrada."
                AuthState.Reason.INITIAL -> return // no message for cold-start unauth
            }
            _sessionEvents.tryEmit(SessionEvent(message = message, reason = reason))
        }
    }

    // --- Internals ---

    /**
     * Persists the JWT bundle returned by login / register and transitions [authState] to
     * [AuthState.Authenticated]. Extracts `tenantId`, `firstName`/`first_name`/`name` and
     * `exp` claims while we have the raw token in hand.
     */
    private suspend fun persistSuccessfulAuth(response: JwtResponse) {
        sessionMutex.withLock {
            tokenStorage.saveToken(response.token)
            tokenStorage.saveUsername(response.username)
            extractAndSaveJwtClaims(response.token)

            val exp = JwtDecoder.extractExpiration(response.token)
            _authState.value = AuthState.Authenticated(response.token, exp)
        }
    }

    /**
     * Extracts tenantId, display name and `exp` from the JWT and saves them.
     */
    private suspend fun extractAndSaveJwtClaims(token: String) {
        // Try common claim names for tenantId
        val tenantId = JwtDecoder.extractLongClaim(token, "tenantId")
            ?: JwtDecoder.extractLongClaim(token, "tenant_id")
            ?: JwtDecoder.extractStringClaim(token, "tenantId")?.toLongOrNull()
            ?: JwtDecoder.extractStringClaim(token, "tenant_id")?.toLongOrNull()
        tenantId?.let { tokenStorage.saveTenantId(it) }

        // Try to extract display name
        val displayName = JwtDecoder.extractStringClaim(token, "firstName")
            ?: JwtDecoder.extractStringClaim(token, "first_name")
            ?: JwtDecoder.extractStringClaim(token, "name")
        displayName?.let { tokenStorage.saveDisplayName(it) }

        // Persist `exp` for fast access from non-suspending consumers (the WebSocket pre-check
        // and the splash route both pull this without re-decoding the JWT).
        JwtDecoder.extractExpiration(token)?.let { tokenStorage.saveTokenExpiry(it) }
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
