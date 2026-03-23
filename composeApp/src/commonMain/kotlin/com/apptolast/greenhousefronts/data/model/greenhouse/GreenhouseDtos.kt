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
 * Response from GET /api/v1/alerts/unresolved/tenant/{tenantId}
 * Only fields needed for counting per greenhouse.
 */
/**
 * Response from GET /api/v1/alerts/unresolved/tenant/{tenantId}
 * Only fields needed for counting per greenhouse.
 */
@Serializable
data class AlertResponse(
    val id: Long,
    val sectorId: Long,
    val clientName: String? = null,
    val isResolved: Boolean = false,
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
