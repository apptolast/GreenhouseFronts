package com.apptolast.greenhousefronts.data.local.auth

/**
 * Persisted auth data for the current user. Backed by multiplatform-settings: SharedPreferences
 * on Android, NSUserDefaults on iOS, java.util.prefs on JVM, localStorage on web.
 */
interface TokenStorage {

    suspend fun saveToken(token: String)
    suspend fun getToken(): String?

    suspend fun saveUsername(username: String)
    suspend fun getUsername(): String?

    suspend fun saveTenantId(tenantId: Long)
    suspend fun getTenantId(): Long?

    suspend fun saveDisplayName(displayName: String)
    suspend fun getDisplayName(): String?

    /**
     * Opaque refresh token. Rotates on every successful `/auth/refresh` — overwrite the
     * previous value or the backend's reuse-detection will revoke the whole family.
     */
    suspend fun saveRefreshToken(token: String)
    suspend fun getRefreshToken(): String?

    /** Removes only the refresh token + expiry; use [clearAll] for full logout. */
    suspend fun clearRefreshToken()

    /**
     * Refresh-token expiry (Unix epoch seconds), computed as `now + refreshExpiresIn`.
     * Read at cold boot to skip a doomed refresh attempt.
     */
    suspend fun saveRefreshExpiry(epochSec: Long)
    suspend fun getRefreshExpiry(): Long?

    /** Wipes everything — call on logout or on a terminal session-invalidation event. */
    suspend fun clearAll()
}

internal object TokenStorageKeys {
    private const val PREFIX = "greenhouse_auth_"
    const val ACCESS_TOKEN = "${PREFIX}access_token"
    const val USERNAME = "${PREFIX}username"
    const val TENANT_ID = "${PREFIX}tenant_id"
    const val DISPLAY_NAME = "${PREFIX}display_name"
    const val REFRESH_TOKEN = "${PREFIX}refresh_token"
    const val REFRESH_EXP = "${PREFIX}refresh_exp"
}
