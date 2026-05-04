package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.alert.AlertActorResponse
import com.apptolast.greenhousefronts.data.model.alert.AlertTransitionResponse
import com.apptolast.greenhousefronts.data.model.greenhouse.AlertResponse
import com.apptolast.greenhousefronts.data.remote.api.AlertApiService
import com.apptolast.greenhousefronts.data.remote.api.AlertHistoryApiService
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusResponse
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import com.apptolast.greenhousefronts.data.remote.websocket.WsAlertResponse
import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.model.AlertActor
import com.apptolast.greenhousefronts.domain.model.AlertTransition
import com.apptolast.greenhousefronts.domain.model.PagedResult
import com.apptolast.greenhousefronts.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AlertRepositoryImpl(
    private val api: AlertApiService,
    private val historyApi: AlertHistoryApiService,
    private val tokenStorage: TokenStorage,
    private val webSocket: GreenhouseStatusWebSocket,
) : AlertRepository {

    override fun observeActiveAlerts(): Flow<List<Alert>> =
        webSocket.statusFlow()
            .map { it.flattenActiveAlerts() }
            // Snapshots arrive at the WS cadence (~10 s); skip emissions where the active
            // set is identical so the UI doesn't recompose pointlessly.
            .distinctUntilChanged()

    override suspend fun getTransitionHistory(page: Int, size: Int): Result<PagedResult<AlertTransition>> =
        runCatching {
            val tenantId = requireTenantId()
            val response = historyApi.getEvents(tenantId = tenantId, page = page, size = size)
            PagedResult(
                items = response.items.map { it.toDomain() },
                page = response.page,
                size = response.size,
                total = response.total,
                hasMore = response.hasMore,
            )
        }

    override suspend fun getById(alertId: Long): Result<Alert> = runCatching {
        api.getById(alertId).toDomain()
    }

    private suspend fun requireTenantId(): Long = tokenStorage.getTenantId()
        ?: error("No tenantId in JWT — user is not properly logged in")

    // --- Mappers ---

    private fun GreenhouseStatusResponse.flattenActiveAlerts(): List<Alert> = buildList {
        tenants.forEach { tenant ->
            tenant.greenhouses.forEach { greenhouse ->
                greenhouse.sectors.forEach { sector ->
                    sector.alerts.asSequence()
                        .filter { !it.isResolved }
                        .forEach { ws ->
                            add(ws.toAlert(sectorId = sector.id, sectorCode = sector.code))
                        }
                }
            }
        }
    }

    private fun WsAlertResponse.toAlert(sectorId: Long, sectorCode: String): Alert {
        val createdAtSafe = createdAt.orEmpty()
        return Alert(
            id = id,
            code = code,
            sectorId = sectorId,
            sectorCode = sectorCode,
            alertTypeName = alertType?.name,
            severityName = severity?.name ?: "INFO",
            severityLevel = severity?.level?.toShort() ?: 1,
            message = message,
            description = description,
            clientName = clientName,
            isResolved = isResolved,
            resolvedAt = resolvedAt,
            resolvedByUserName = resolvedByUser?.username,
            createdAt = createdAtSafe,
            // WS frame doesn't carry updatedAt — fall back to createdAt; the field isn't
            // currently rendered, only kept for parity with the REST shape.
            updatedAt = createdAtSafe,
        )
    }

    private fun AlertResponse.toDomain(): Alert = Alert(
        id = id,
        code = code,
        sectorId = sectorId,
        sectorCode = sectorCode,
        alertTypeName = alertTypeName,
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

    private fun AlertTransitionResponse.toDomain(): AlertTransition = AlertTransition(
        transitionId = transitionId,
        at = at,
        toResolved = toResolved,
        source = source,
        actor = actor.toDomain(),
        alertId = alertId,
        alertCode = alertCode,
        alertMessage = alertMessage,
        alertTypeName = alertTypeName,
        severityName = severityName ?: "INFO",
        severityLevel = severityLevel ?: 1,
        sectorId = sectorId,
        sectorCode = sectorCode,
        occurrenceNumber = occurrenceNumber,
    )

    private fun AlertActorResponse.toDomain(): AlertActor = AlertActor(
        kind = when (kind.uppercase()) {
            "USER" -> AlertActor.ActorKind.USER
            "DEVICE" -> AlertActor.ActorKind.DEVICE
            "SYSTEM" -> AlertActor.ActorKind.SYSTEM
            else -> AlertActor.ActorKind.UNKNOWN
        },
        userId = userId,
        username = username,
        displayName = displayName,
        ref = ref,
    )
}
