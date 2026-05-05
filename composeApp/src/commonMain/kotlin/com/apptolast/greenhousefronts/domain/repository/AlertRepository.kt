package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.model.AlertTransition
import com.apptolast.greenhousefronts.domain.model.PagedResult
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    /**
     * Hot stream of currently-active (unresolved) alerts. Sourced from the WebSocket
     * snapshot — single source of truth, no REST round-trip, no race condition with
     * server-pushed state changes.
     */
    fun observeActiveAlerts(): Flow<List<Alert>>

    /**
     * Tenant-wide history of alert state transitions, paginated. Defaults to the last
     * 30 days (server-side default for the `from`/`to` query params we don't send).
     *
     * Each row is a single state change — the same `alertId` may appear multiple times.
     *
     * Pass [severityIds] to filter server-side; empty list returns all severities.
     */
    suspend fun getTransitionHistory(
        page: Int = 0,
        size: Int = 50,
        severityIds: List<Short> = emptyList(),
    ): Result<PagedResult<AlertTransition>>

    /** Single-alert lookup. Used by FCM deep links to decide which tab to open. */
    suspend fun getById(alertId: Long): Result<Alert>
}
