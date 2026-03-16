package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.Greenhouse

/**
 * Repository interface for greenhouse operations.
 */
interface GreenhouseRepository {

    /**
     * Fetches all greenhouses for the current tenant,
     * enriched with sector and alert counts.
     */
    suspend fun getGreenhouses(): Result<List<Greenhouse>>

    /**
     * Fetches a single greenhouse with full sector names and alert count.
     */
    suspend fun getGreenhouseDetail(greenhouseId: Long): Result<Greenhouse>

    /**
     * Toggles a greenhouse's active status.
     */
    suspend fun setGreenhouseActive(greenhouseId: Long, isActive: Boolean): Result<Greenhouse>
}
