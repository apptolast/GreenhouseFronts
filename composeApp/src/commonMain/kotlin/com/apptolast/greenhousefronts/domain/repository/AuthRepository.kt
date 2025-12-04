package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest

/**
 * Repository interface for authentication operations.
 * Defines the contract for login, registration, and session management.
 */
interface AuthRepository {

    /**
     * Authenticates a user with email and password.
     * On success, stores the JWT token securely.
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing JWT response on success, or AuthError on failure
     */
    suspend fun login(email: String, password: String): Result<JwtResponse>

    /**
     * Registers a new tenant and admin user.
     * On success, stores the JWT token securely (auto-login).
     *
     * @param request Registration data including company and user information
     * @return Result containing JWT response on success, or AuthError on failure
     */
    suspend fun register(request: RegisterRequest): Result<JwtResponse>

    /**
     * Requests a password reset email.
     *
     * @param email The email address to send the reset token to
     * @return Result success if the request was sent successfully (or if the user doesn't exist, for security)
     */
    suspend fun forgotPassword(email: String): Result<Unit>

    /**
     * Resets the password using a token.
     *
     * @param token The reset token received via email
     * @param password The new password
     * @return Result success if password was reset
     */
    suspend fun resetPassword(token: String, password: String): Result<Unit>

    /**
     * Logs out the current user by clearing stored credentials.
     */
    suspend fun logout()

    /**
     * Checks if a user is currently logged in.
     * Note: Only checks for token presence, not validity.
     *
     * @return true if a token exists in storage
     */
    fun isLoggedIn(): Boolean

    /**
     * Retrieves the stored access token.
     *
     * @return The JWT token or null if not authenticated
     */
    suspend fun getToken(): String?

    /**
     * Retrieves the stored username.
     *
     * @return The username or null if not authenticated
     */
    suspend fun getUsername(): String?
}
