package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.greenhouse.AlertResponse
import com.apptolast.greenhousefronts.data.remote.api.AlertApiService
import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.repository.AlertRepository

class AlertRepositoryImpl(
    private val api: AlertApiService,
    private val tokenStorage: TokenStorage,
) : AlertRepository {

    override suspend fun getActive(): Result<List<Alert>> = listFiltered(isResolved = false)

    override suspend fun getHistory(): Result<List<Alert>> = listFiltered(isResolved = true)

    override suspend fun getById(alertId: Long): Result<Alert> = runCatching {
        api.getById(alertId).toDomain()
    }

    private suspend fun listFiltered(isResolved: Boolean): Result<List<Alert>> = runCatching {
        val tenantId = tokenStorage.getTenantId()
            ?: error("No tenantId in JWT — user is not properly logged in")
        api.listForTenant(tenantId = tenantId, isResolved = isResolved)
            .map { it.toDomain() }
    }

    private fun AlertResponse.toDomain(): Alert = Alert(
        id = id,
        code = code,
        sectorId = sectorId,
        sectorCode = sectorCode,
        alertTypeName = alertTypeName,
        // Defensive default: an unknown severity is treated as INFO upstream by AlertSeverity.fromName.
        severityName = severityName ?: "INFO",
        severityLevel = severityLevel ?: 1,
        message = message,
        description = description,
        clientName = clientName,
        isResolved = isResolved,
        resolvedAt = resolvedAt,
        resolvedByUserName = resolvedByUserName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
