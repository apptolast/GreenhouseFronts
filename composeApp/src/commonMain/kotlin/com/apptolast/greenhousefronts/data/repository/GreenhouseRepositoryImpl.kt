package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.greenhouse.GreenhouseUpdateRequest
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository

/**
 * Implementation of [GreenhouseRepository].
 *
 * Read path: only [getGreenhouses] for the list screen aggregates greenhouses + sectors +
 * unresolved-alert counts via three REST calls. The detail screen does NOT use REST for
 * reads — it reflects the WebSocket snapshot directly. Avoiding the extra REST round-trip
 * on detail entry removes a long-standing race between REST and the STOMP push that both
 * raced to populate the same `uiState`.
 *
 * Write path: [setGreenhouseActive] only fires the PUT. The backend then broadcasts a
 * `GREENHOUSE_CRUD` event over STOMP, so the active subscribers (detail screen) see the
 * new state without a second REST hit.
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
                        ?.sorted()
                        ?: emptyList(),
                )
            }.sortedBy { it.name }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setGreenhouseActive(
        greenhouseId: Long,
        isActive: Boolean,
    ): Result<Unit> {
        val tenantId = tokenStorage.getTenantId()
            ?: return Result.failure(Exception("No se encontró el ID del tenant"))

        return try {
            apiService.updateGreenhouse(
                tenantId = tenantId,
                greenhouseId = greenhouseId,
                request = GreenhouseUpdateRequest(isActive = isActive),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
