package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.tenant.TenantResponse
import com.apptolast.greenhousefronts.data.model.user.UserResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * API service for user operations.
 * Uses the authenticated HttpClient (Bearer token auto-injected).
 *
 * @param httpClient Injected authenticated HTTP client
 */
class UserApiService(
    private val httpClient: HttpClient,
) {

    /**
     * Lists all users for a tenant.
     * GET /api/v1/tenants/{tenantId}/users
     */
    suspend fun getUsers(tenantId: Long): List<UserResponse> {
        return httpClient.get("$baseUrl/tenants/$tenantId/users").body()
    }

    /**
     * Gets tenant info.
     * GET /api/v1/tenants/{tenantId}
     */
    suspend fun getTenant(tenantId: Long): TenantResponse {
        return httpClient.get("$baseUrl/tenants/$tenantId").body()
    }
}
