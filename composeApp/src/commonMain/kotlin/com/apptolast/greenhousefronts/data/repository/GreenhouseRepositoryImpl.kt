package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository

/**
 * Implementation of GreenhouseRepository.
 * Aggregates greenhouse, sector, and alert data from multiple API calls.
 *
 * @param apiService API service for greenhouse endpoints
 * @param tokenStorage Token storage to retrieve tenantId
 */
class GreenhouseRepositoryImpl(
    private val apiService: GreenhouseApiService,
    private val tokenStorage: TokenStorage,
) : GreenhouseRepository {

    override suspend fun getGreenhouses(): Result<List<Greenhouse>> {
        val tenantId = tokenStorage.getTenantId()
            ?: return Result.failure(Exception("No se encontró el ID del tenant"))

        return try {
            val greenhouses = apiService.getGreenhouses(tenantId)

            // Fetch sectors and alerts in parallel for enrichment
            val sectors = runCatching { apiService.getSectors(tenantId) }.getOrDefault(emptyList())
            val alerts = runCatching { apiService.getUnresolvedAlerts(tenantId) }.getOrDefault(emptyList())

            // Group sectors by greenhouse
            val sectorsByGreenhouse = sectors.groupBy { it.greenhouseId }

            // Map alerts to greenhouses through sectors
            val sectorToGreenhouse = sectors.associate { it.id to it.greenhouseId }
            val alertsByGreenhouse = alerts.groupBy { sectorToGreenhouse[it.sectorId] }

            val result = greenhouses.map { dto ->
                Greenhouse(
                    id = dto.id,
                    code = dto.code,
                    name = dto.name,
                    isActive = dto.isActive,
                    areaM2 = dto.areaM2,
                    sectorCount = sectorsByGreenhouse[dto.id]?.size ?: 0,
                    alertCount = alertsByGreenhouse[dto.id]?.size ?: 0,
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
