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
}
