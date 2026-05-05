package com.apptolast.greenhousefronts.data.model.greenhouse

import kotlinx.serialization.Serializable

/**
 * Response from GET /api/v1/tenants/{tenantId}/greenhouses
 */
@Serializable
data class GreenhouseResponse(
    val id: Long,
    val code: String,
    val name: String,
    val tenantId: Long,
    val location: LocationDto? = null,
    val areaM2: Double? = null,
    val timezone: String? = null,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class LocationDto(
    val lat: Double? = null,
    val lon: Double? = null,
)

/**
 * Response from GET /api/v1/tenants/{tenantId}/sectors
 */
@Serializable
data class SectorResponse(
    val id: Long,
    val code: String,
    val tenantId: Long,
    val greenhouseId: Long,
    val greenhouseCode: String? = null,
    val name: String? = null,
)

/**
 * Response from `GET /api/v1/alerts*` endpoints (full listing, by id, unresolved by tenant…).
 *
 * Mirrors `apptolast/InvernaderosAPI` `features/alert/dto/response/AlertResponse.kt`. All
 * fields except the IDs and timestamps are nullable to tolerate partial responses from older
 * backend builds and to keep the existing `getUnresolvedAlerts` call site (which only uses
 * `id`, `sectorId`, `clientName`, `isResolved`) working unchanged.
 */
@Serializable
data class AlertResponse(
    val id: Long,
    val code: String = "",
    val tenantId: Long? = null,
    val sectorId: Long,
    val sectorCode: String? = null,
    val alertTypeId: Short? = null,
    val alertTypeName: String? = null,
    val severityId: Short? = null,
    val severityName: String? = null,
    val severityLevel: Short? = null,
    val message: String? = null,
    val description: String? = null,
    val clientName: String? = null,
    val isResolved: Boolean = false,
    val resolvedAt: String? = null,
    val resolvedByUserId: Long? = null,
    val resolvedByUserName: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

/**
 * Request for PUT /api/v1/tenants/{tenantId}/greenhouses/{greenhouseId}
 * All fields are nullable — partial update (null fields are not modified).
 */
@Serializable
data class GreenhouseUpdateRequest(
    val name: String? = null,
    val isActive: Boolean? = null,
    val areaM2: Double? = null,
    val timezone: String? = null,
)
