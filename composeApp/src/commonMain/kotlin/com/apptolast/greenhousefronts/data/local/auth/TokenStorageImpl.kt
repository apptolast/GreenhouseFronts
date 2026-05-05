package com.apptolast.greenhousefronts.data.local.auth

import com.russhwolf.settings.Settings

class TokenStorageImpl : TokenStorage {

    private val settings: Settings = Settings()

    override suspend fun saveToken(token: String) {
        settings.putString(TokenStorageKeys.ACCESS_TOKEN, token)
    }

    override suspend fun getToken(): String? =
        settings.getStringOrNull(TokenStorageKeys.ACCESS_TOKEN)

    override suspend fun saveUsername(username: String) {
        settings.putString(TokenStorageKeys.USERNAME, username)
    }

    override suspend fun getUsername(): String? =
        settings.getStringOrNull(TokenStorageKeys.USERNAME)

    override suspend fun saveTenantId(tenantId: Long) {
        settings.putLong(TokenStorageKeys.TENANT_ID, tenantId)
    }

    override suspend fun getTenantId(): Long? =
        if (settings.hasKey(TokenStorageKeys.TENANT_ID)) {
            settings.getLong(TokenStorageKeys.TENANT_ID, 0L)
        } else {
            null
        }

    override suspend fun saveDisplayName(displayName: String) {
        settings.putString(TokenStorageKeys.DISPLAY_NAME, displayName)
    }

    override suspend fun getDisplayName(): String? =
        settings.getStringOrNull(TokenStorageKeys.DISPLAY_NAME)

    override suspend fun saveRefreshToken(token: String) {
        settings.putString(TokenStorageKeys.REFRESH_TOKEN, token)
    }

    override suspend fun getRefreshToken(): String? =
        settings.getStringOrNull(TokenStorageKeys.REFRESH_TOKEN)

    override suspend fun clearRefreshToken() {
        settings.remove(TokenStorageKeys.REFRESH_TOKEN)
        settings.remove(TokenStorageKeys.REFRESH_EXP)
    }

    override suspend fun saveRefreshExpiry(epochSec: Long) {
        settings.putLong(TokenStorageKeys.REFRESH_EXP, epochSec)
    }

    override suspend fun getRefreshExpiry(): Long? =
        if (settings.hasKey(TokenStorageKeys.REFRESH_EXP)) {
            settings.getLong(TokenStorageKeys.REFRESH_EXP, 0L)
        } else {
            null
        }

    override suspend fun clearAll() {
        settings.remove(TokenStorageKeys.ACCESS_TOKEN)
        settings.remove(TokenStorageKeys.USERNAME)
        settings.remove(TokenStorageKeys.TENANT_ID)
        settings.remove(TokenStorageKeys.DISPLAY_NAME)
        settings.remove(TokenStorageKeys.REFRESH_TOKEN)
        settings.remove(TokenStorageKeys.REFRESH_EXP)
    }
}
