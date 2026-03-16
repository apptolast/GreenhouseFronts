package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.greenhouse.GreenhouseUpdateRequest
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
            val sectors = runCatching { apiService.getSectors(tenantId) }.getOrDefault(emptyList())
            val alerts = runCatching { apiService.getUnresolvedAlerts(tenantId) }.getOrDefault(emptyList())

            val sectorsByGreenhouse = sectors.groupBy { it.greenhouseId }
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
                    sectorNames = sectorsByGreenhouse[dto.id]
                        ?.mapNotNull { it.name }
                        ?: emptyList(),
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGreenhouseDetail(greenhouseId: Long): Result<Greenhouse> {
        val tenantId = tokenStorage.getTenantId()
            ?: return Result.failure(Exception("No se encontró el ID del tenant"))

        return try {
            val greenhouses = apiService.getGreenhouses(tenantId)
            val dto = greenhouses.find { it.id == greenhouseId }
                ?: return Result.failure(Exception("Invernadero no encontrado"))

            val sectors = runCatching { apiService.getSectors(tenantId) }.getOrDefault(emptyList())
            val alerts = runCatching { apiService.getUnresolvedAlerts(tenantId) }.getOrDefault(emptyList())

            val greenhouseSectors = sectors.filter { it.greenhouseId == greenhouseId }
            val greenhouseSectorIds = greenhouseSectors.map { it.id }.toSet()
            val greenhouseAlerts = alerts.filter { it.sectorId in greenhouseSectorIds }

            Result.success(
                Greenhouse(
                    id = dto.id,
                    code = dto.code,
                    name = dto.name,
                    isActive = dto.isActive,
                    areaM2 = dto.areaM2,
                    sectorCount = greenhouseSectors.size,
                    alertCount = greenhouseAlerts.size,
                    sectorNames = greenhouseSectors.mapNotNull { it.name },
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setGreenhouseActive(
        greenhouseId: Long,
        isActive: Boolean,
    ): Result<Greenhouse> {
        val tenantId = tokenStorage.getTenantId()
            ?: return Result.failure(Exception("No se encontró el ID del tenant"))

        return try {
            val response = apiService.updateGreenhouse(
                tenantId = tenantId,
                greenhouseId = greenhouseId,
                request = GreenhouseUpdateRequest(isActive = isActive),
            )

            // Re-fetch full detail to get sector/alert counts
            getGreenhouseDetail(response.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
