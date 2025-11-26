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

    override suspend fun clearAll() {
        settings.remove(TokenStorageKeys.ACCESS_TOKEN)
        settings.remove(TokenStorageKeys.USERNAME)
    }

    override fun hasToken(): Boolean {
        return settings.hasKey(TokenStorageKeys.ACCESS_TOKEN)
    }
}
