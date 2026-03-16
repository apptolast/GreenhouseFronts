package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.greenhouse.AlertResponse
import com.apptolast.greenhousefronts.data.model.greenhouse.GreenhouseResponse
import com.apptolast.greenhousefronts.data.model.greenhouse.GreenhouseUpdateRequest
import com.apptolast.greenhousefronts.data.model.greenhouse.SectorResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * API service for greenhouse operations.
 * Uses the authenticated HttpClient (Bearer token auto-injected).
 *
 * @param httpClient Injected authenticated HTTP client
 */
class GreenhouseApiService(
    private val httpClient: HttpClient,
) {

    /**
     * Lists all greenhouses for a tenant.
     * GET /api/v1/tenants/{tenantId}/greenhouses
     */
    suspend fun getGreenhouses(tenantId: Long): List<GreenhouseResponse> {
        return httpClient.get("$baseUrl/tenants/$tenantId/greenhouses").body()
    }

    /**
     * Lists all sectors for a tenant.
     * GET /api/v1/tenants/{tenantId}/sectors
     */
    suspend fun getSectors(tenantId: Long): List<SectorResponse> {
        return httpClient.get("$baseUrl/tenants/$tenantId/sectors").body()
    }

    /**
     * Lists unresolved alerts for a tenant.
     * GET /api/v1/alerts/unresolved/tenant/{tenantId}
     */
    suspend fun getUnresolvedAlerts(tenantId: Long): List<AlertResponse> {
        return httpClient.get("$baseUrl/alerts/unresolved/tenant/$tenantId").body()
    }

    /**
     * Updates a greenhouse (partial update — null fields are ignored).
     * PUT /api/v1/tenants/{tenantId}/greenhouses/{greenhouseId}
     */
    suspend fun updateGreenhouse(
        tenantId: Long,
        greenhouseId: Long,
        request: GreenhouseUpdateRequest,
    ): GreenhouseResponse {
        return httpClient.put("$baseUrl/tenants/$tenantId/greenhouses/$greenhouseId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
