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
     * Saves the tenant ID extracted from the JWT token.
     * @param tenantId The tenant's numeric ID
     */
    suspend fun saveTenantId(tenantId: Long)

    /**
     * Retrieves the stored tenant ID.
     * @return The tenant ID or null
     */
    suspend fun getTenantId(): Long?

    /**
     * Saves the user display name for greeting UI.
     * @param displayName The user's first name or display name
     */
    suspend fun saveDisplayName(displayName: String)

    /**
     * Retrieves the stored display name.
     * @return The display name or null
     */
    suspend fun getDisplayName(): String?

    /**
     * Persists the access-token expiration (Unix epoch seconds, RFC 7519 `exp` claim).
     * Stored alongside the token on login / register so [getTokenExpiry] is fast and does
     * not require re-decoding the JWT on every check.
     */
    suspend fun saveTokenExpiry(epochSec: Long)

    /**
     * Retrieves the stored access-token expiration in Unix epoch seconds, or null if no
     * token has been persisted with an expiry yet.
     */
    suspend fun getTokenExpiry(): Long?

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
    const val TENANT_ID = "${PREFIX}tenant_id"
    const val DISPLAY_NAME = "${PREFIX}display_name"
    const val TOKEN_EXP = "${PREFIX}token_exp"
}
