package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.auth.AuthError
import com.apptolast.greenhousefronts.data.model.auth.ForgotPasswordRequest
import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.LoginRequest
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.data.model.auth.ResetPasswordRequest
import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

/**
 * Implementation of AuthRepository.
 * Handles authentication operations and token management.
 *
 * @param authApiService API service for auth endpoints
 * @param tokenStorage Secure storage for JWT tokens
 */
class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<JwtResponse> {
        return try {
            val request = LoginRequest(
                username = email.trim(),
                password = password
            )
            val response = authApiService.login(request)

            // Store token and username
            tokenStorage.saveToken(response.token)
            tokenStorage.saveUsername(response.username)

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

            // Store token and username (auto-login)
            tokenStorage.saveToken(response.token)
            tokenStorage.saveUsername(response.username)

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
        } catch (e: ClientRequestException) {
            // Generally we return success even if email doesn't exist for security,
            // unless the backend explicitly returns an error we want to show.
            // Assuming backend returns 200 even if user not found, or 400/404 if we want to handle it.
            // The backend code provided says: @ApiResponse(responseCode = "200", description = "Email sent if user exists")
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
        tokenStorage.clearAll()
    }

    override fun isLoggedIn(): Boolean {
        return tokenStorage.hasToken()
    }

    override suspend fun getToken(): String? {
        return tokenStorage.getToken()
    }

    override suspend fun getUsername(): String? {
        return tokenStorage.getUsername()
    }

    /**
     * Maps Ktor ClientRequestException to specific AuthError types.
     */
    private suspend fun mapClientError(e: ClientRequestException): AuthError {
        return when (e.response.status) {
            HttpStatusCode.Unauthorized -> {
                AuthError.InvalidCredentials()
            }

            HttpStatusCode.BadRequest -> {
                val body = try {
                    e.response.bodyAsText()
                } catch (ex: Exception) {
                    println("Failed to parse error response body: ${ex.message}")
                    "Unable to parse error response"
                }

                when {
                    body.contains("Email already in use", ignoreCase = true) ||
                            body.contains("email", ignoreCase = true) && body.contains(
                        "use",
                        ignoreCase = true
                    ) -> {
                        AuthError.EmailAlreadyInUse()
                    }

                    else -> {
                        AuthError.ValidationError(
                            message = body.ifBlank { "Datos de registro inválidos" }
                        )
                    }
                }
            }

            else -> {
                AuthError.Unknown("Error ${e.response.status.value}: ${e.message}")
            }
        }
    }
}
