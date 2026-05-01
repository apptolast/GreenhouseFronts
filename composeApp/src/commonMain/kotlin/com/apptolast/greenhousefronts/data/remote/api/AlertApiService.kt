package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.greenhouse.AlertResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * REST client for the alerts catalogue.
 *
 *  - [listForTenant] hits `GET /api/v1/alerts?tenantId=…&isResolved=…&limit=…`. Used by the
 *    Alerts screen to render the Active tab.
 *  - [listHistory] hits `GET /api/v1/alerts/history/tenant/{tenantId}?limit=…`. Returns the
 *    full alert feed (active + resolved) ordered by createdAt DESC — the dedicated endpoint
 *    for the History tab. The legacy `?isResolved=true` filter would only return resolved
 *    alerts, leaving the tab empty until something gets resolved in production.
 *  - [getById] is the fallback when a deep-link arrives for an alert that isn't in the
 *    currently loaded list (e.g. tap on a notification while viewing History).
 */
class AlertApiService(
    private val httpClient: HttpClient,
) {
    suspend fun listForTenant(
        tenantId: Long,
        isResolved: Boolean,
        limit: Int = DEFAULT_LIMIT,
    ): List<AlertResponse> =
        httpClient.get("$baseUrl/alerts") {
            parameter("tenantId", tenantId)
            parameter("isResolved", isResolved)
            parameter("limit", limit)
        }.body()

    suspend fun listHistory(
        tenantId: Long,
        limit: Int = DEFAULT_LIMIT,
    ): List<AlertResponse> =
        httpClient.get("$baseUrl/alerts/history/tenant/$tenantId") {
            parameter("limit", limit)
        }.body()

    suspend fun getById(alertId: Long): AlertResponse =
        httpClient.get("$baseUrl/alerts/$alertId").body()

    companion object {
        const val DEFAULT_LIMIT = 100
    }
}
