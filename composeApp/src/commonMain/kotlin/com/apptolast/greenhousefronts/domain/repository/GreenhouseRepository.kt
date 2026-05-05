package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.Greenhouse

/**
 * Repository interface for greenhouse operations.
 *
 * The detail screen does **not** go through this repository to read greenhouse data —
 * it consumes the WebSocket [com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket]
 * statusFlow as the single source of truth (sectors, devices, alerts and the greenhouse
 * itself all arrive in the same snapshot). REST is reserved for the list screen and for
 * write operations, where the backend's WebSocket push then echoes the change back.
 */
interface GreenhouseRepository {

    /**
     * Fetches all greenhouses for the current tenant,
     * enriched with sector and alert counts. Used by the list screen.
     */
    suspend fun getGreenhouses(): Result<List<Greenhouse>>

    /**
     * Toggles a greenhouse's active status (PUT only).
     *
     * On success the backend emits a `GREENHOUSE_CRUD` STOMP push that any subscribed
     * detail screen will pick up to refresh the UI. Callers should apply an optimistic
     * update before invoking this and rely on the WebSocket round-trip for the final
     * source of truth — no extra REST refetch is performed here.
     */
    suspend fun setGreenhouseActive(greenhouseId: Long, isActive: Boolean): Result<Unit>
}
