package com.apptolast.greenhousefronts.data.local.auth

import com.russhwolf.settings.Settings

/**
 * Implementation of TokenStorage using multiplatform-settings.
 * Uses the default Settings() factory which provides:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 * - Desktop: Java Preferences
 * - Web: localStorage
 */
class TokenStorageImpl : TokenStorage {

    private val settings: Settings = Settings()

    override suspend fun saveToken(token: String) {
        settings.putString(TokenStorageKeys.ACCESS_TOKEN, token)
    }

    override suspend fun getToken(): String? {
        return settings.getStringOrNull(TokenStorageKeys.ACCESS_TOKEN)
    }

    override suspend fun saveUsername(username: String) {
        settings.putString(TokenStorageKeys.USERNAME, username)
    }

    override suspend fun getUsername(): String? {
        return settings.getStringOrNull(TokenStorageKeys.USERNAME)
    }

    override suspend fun saveTenantId(tenantId: Long) {
        settings.putLong(TokenStorageKeys.TENANT_ID, tenantId)
    }

    override suspend fun getTenantId(): Long? {
        return if (settings.hasKey(TokenStorageKeys.TENANT_ID)) {
            settings.getLong(TokenStorageKeys.TENANT_ID, 0L)
        } else {
            null
        }
    }

    override suspend fun saveDisplayName(displayName: String) {
        settings.putString(TokenStorageKeys.DISPLAY_NAME, displayName)
    }

    override suspend fun getDisplayName(): String? {
        return settings.getStringOrNull(TokenStorageKeys.DISPLAY_NAME)
    }

    override suspend fun clearAll() {
        settings.remove(TokenStorageKeys.ACCESS_TOKEN)
        settings.remove(TokenStorageKeys.USERNAME)
        settings.remove(TokenStorageKeys.TENANT_ID)
        settings.remove(TokenStorageKeys.DISPLAY_NAME)
    }

    override fun hasToken(): Boolean {
        return settings.hasKey(TokenStorageKeys.ACCESS_TOKEN)
    }
}
