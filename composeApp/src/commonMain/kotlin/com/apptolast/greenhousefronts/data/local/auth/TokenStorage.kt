package com.apptolast.greenhousefronts.data.local.auth

/**
 * Interface for JWT token storage.
 * Uses multiplatform-settings library with default platform implementations:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 * - Desktop: Java Preferences
 * - Web: localStorage
 */
interface TokenStorage {

    /**
     * Saves the JWT access token.
     * @param token The JWT token string to store
     */
    suspend fun saveToken(token: String)

    /**
     * Retrieves the stored access token.
     * @return The stored token or null if not authenticated
     */
    suspend fun getToken(): String?

    /**
     * Saves the authenticated username for display purposes.
     * @param username The username/email of the authenticated user
     */
    suspend fun saveUsername(username: String)

    /**
     * Retrieves the stored username.
     * @return The stored username or null
     */
    suspend fun getUsername(): String?

    /**
     * Clears all stored authentication data.
     * Should be called during logout.
     */
    suspend fun clearAll()

    /**
     * Checks if a valid token is currently stored.
     * Note: Does not validate token expiration, only presence.
     * @return true if a token exists
     */
    fun hasToken(): Boolean
}

/**
 * Storage keys for authentication data.
 * Using a prefix to avoid key collisions.
 */
internal object TokenStorageKeys {
    private const val PREFIX = "greenhouse_auth_"
    const val ACCESS_TOKEN = "${PREFIX}access_token"
    const val USERNAME = "${PREFIX}username"
}
