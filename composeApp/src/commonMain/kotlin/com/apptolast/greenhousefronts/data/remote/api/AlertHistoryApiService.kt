package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.alert.AlertTransitionResponse
import com.apptolast.greenhousefronts.data.model.alert.PagedResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Tenant-wide history of alert state transitions.
 *
 * The backend defaults the time window to the last 30 days when `from`/`to` are omitted —
 * we currently don't expose those filters from the UI, so we rely on that default.
 *
 * Severity filtering is server-side: pass one or more `severityId` values and Spring's
 * Pageable / `@RequestParam List<Short>` binder collects them via repeated query params.
 * Empty list → no filter (the default behavior, all severities returned).
 */
class AlertHistoryApiService(
    private val httpClient: HttpClient,
) {
    suspend fun getEvents(
        tenantId: Long,
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
        severityIds: List<Short> = emptyList(),
    ): PagedResponse<AlertTransitionResponse> =
        httpClient.get("$baseUrl/tenants/$tenantId/alert-events") {
            parameter("page", page)
            parameter("size", size)
            severityIds.forEach { parameter("severityId", it) }
        }.body()

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
