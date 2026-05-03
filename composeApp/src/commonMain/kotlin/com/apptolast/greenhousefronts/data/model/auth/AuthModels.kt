package com.apptolast.greenhousefronts.data.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for user login.
 * Matches the backend's LoginRequest DTO.
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Request body for new user/tenant registration.
 * Matches the backend's RegisterRequest DTO.
 *
 * Required fields:
 * - companyName: 2-100 characters
 * - taxId: Company tax identification
 * - email: Valid email format
 * - password: Minimum 6 characters
 * - firstName: Contact person's first name
 * - lastName: Contact person's last name
 *
 * Optional fields:
 * - phone: Contact phone number
 * - address: Physical address
 */
@Serializable
data class RegisterRequest(
    @SerialName("company_name")
    val companyName: String,
    @SerialName("tax_id")
    val taxId: String,
    val email: String,
    val password: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val phone: String? = null,
    val address: String? = null
)

/**
 * Response from `/auth/login`, `/auth/register` and `/auth/refresh`.
 *
 * `token`, `type`, `username` and `roles` are required by the OpenAPI schema.
 * `refreshToken`, `expiresIn` and `refreshExpiresIn` are nullable because the backend
 * exposes a kill-switch (`REFRESH_TOKEN_ENABLED`) that, when off, returns the legacy
 * shape. In normal operation they are always informed.
 *
 *  - `expiresIn`         — access token TTL in seconds (default backend config: 3600).
 *  - `refreshExpiresIn`  — refresh token TTL in seconds (default backend config: 2_592_000 = 30d).
 *  - `refreshToken`      — opaque (NOT a JWT). Rotates on every successful `/auth/refresh`.
 */
@Serializable
data class JwtResponse(
    val token: String,
    val type: String = "Bearer",
    val username: String,
    val roles: List<String> = emptyList(),
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val refreshExpiresIn: Long? = null,
)

/**
 * Request body for `POST /auth/refresh`. Identifies the user by the opaque token —
 * no `Authorization` header is required (and Ktor's bearer plugin must not attach one).
 */
@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

/**
 * Request object for forgot password.
 * Matches the backend's ForgotPasswordRequest DTO.
 */
@Serializable
data class ForgotPasswordRequest(
    val email: String
)

/**
 * Request object for reset password.
 * Matches the backend's ResetPasswordRequest DTO.
 */
@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

/**
 * Sealed class representing authentication errors.
 * Used for type-safe error handling in the auth flow.
 */
sealed class AuthError(override val message: String) : Exception(message) {

    /**
     * Invalid username or password (HTTP 401).
     */
    class InvalidCredentials(
        message: String = "Credenciales inválidas"
    ) : AuthError(message)

    /**
     * Email is already registered (HTTP 400 with specific message).
     */
    class EmailAlreadyInUse(
        message: String = "El email ya está en uso"
    ) : AuthError(message)

    /**
     * Form validation error (HTTP 400).
     */
    class ValidationError(
        message: String,
        val field: String? = null
    ) : AuthError(message)

    /**
     * Network connectivity error.
     */
    class NetworkError(
        message: String = "Error de conexión"
    ) : AuthError(message)

    /**
     * Unexpected error.
     */
    class Unknown(
        message: String = "Error desconocido"
    ) : AuthError(message)
}
