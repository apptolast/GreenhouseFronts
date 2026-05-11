package com.apptolast.greenhousefronts.testutil

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage

/**
 * In-memory [TokenStorage] for tests. Tracks individual fields so tests can assert
 * what was persisted without going through `multiplatform-settings`.
 *
 * Counters expose how many writes / clears happened (useful for verifying that
 * `clearRefreshToken` was called on a 401 refresh failure, etc.).
 */
class InMemoryTokenStorage(
    initialAccessToken: String? = null,
    initialRefreshToken: String? = null,
    initialRefreshExpiry: Long? = null,
) : TokenStorage {

    private var accessToken: String? = initialAccessToken
    private var refreshToken: String? = initialRefreshToken
    private var refreshExpiry: Long? = initialRefreshExpiry
    private var username: String? = null
    private var tenantId: Long? = null
    private var displayName: String? = null

    var saveTokenCount = 0
        private set
    var saveRefreshTokenCount = 0
        private set
    var clearAllCount = 0
        private set
    var clearRefreshTokenCount = 0
        private set

    override suspend fun saveToken(token: String) {
        accessToken = token
        saveTokenCount++
    }

    override suspend fun getToken(): String? = accessToken

    override suspend fun saveUsername(username: String) {
        this.username = username
    }

    override suspend fun getUsername(): String? = username

    override suspend fun saveTenantId(tenantId: Long) {
        this.tenantId = tenantId
    }

    override suspend fun getTenantId(): Long? = tenantId

    override suspend fun saveDisplayName(displayName: String) {
        this.displayName = displayName
    }

    override suspend fun getDisplayName(): String? = displayName

    override suspend fun saveRefreshToken(token: String) {
        refreshToken = token
        saveRefreshTokenCount++
    }

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun clearRefreshToken() {
        refreshToken = null
        refreshExpiry = null
        clearRefreshTokenCount++
    }

    override suspend fun saveRefreshExpiry(epochSec: Long) {
        refreshExpiry = epochSec
    }

    override suspend fun getRefreshExpiry(): Long? = refreshExpiry

    override suspend fun clearAll() {
        accessToken = null
        refreshToken = null
        refreshExpiry = null
        username = null
        tenantId = null
        displayName = null
        clearAllCount++
    }
}
