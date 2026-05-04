package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.greenhouse.AlertResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Single-alert lookup. Used by the FCM deep-link path: when the user taps a notification
 * we need to know if the target alert is currently resolved, to land them on the right tab.
 *
 * Active-alerts list is sourced from the WebSocket snapshot — see
 * [com.apptolast.greenhousefronts.data.repository.AlertRepositoryImpl.observeActiveAlerts].
 *
 * History list is sourced from
 * [AlertHistoryApiService] (`/tenants/{tenantId}/alert-events`).
 */
class AlertApiService(
    private val httpClient: HttpClient,
) {
    suspend fun getById(alertId: Long): AlertResponse =
        httpClient.get("$baseUrl/alerts/$alertId").body()
}
