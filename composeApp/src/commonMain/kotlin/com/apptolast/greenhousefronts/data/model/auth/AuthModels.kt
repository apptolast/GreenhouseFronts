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
 * Response from login/register endpoints containing JWT token.
 * The token should be stored securely and sent with subsequent requests.
 */
@Serializable
data class JwtResponse(
    val token: String,
    val type: String = "Bearer",
    val username: String,
    val roles: List<String> = emptyList()
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
