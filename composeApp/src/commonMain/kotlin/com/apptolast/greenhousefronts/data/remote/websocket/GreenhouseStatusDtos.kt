package com.apptolast.greenhousefronts.data.remote.websocket

import kotlinx.serialization.Serializable

/**
 * Root response from WebSocket STOMP `/user/queue/status/response`.
 * Contains the complete business hierarchy with real-time values from TimescaleDB.
 */
@Serializable
data class GreenhouseStatusResponse(
    val timestamp: String,
    val tenants: List<WsTenantResponse> = emptyList(),
)

@Serializable
data class WsTenantResponse(
    val id: Long,
    val code: String,
    val name: String,
    val email: String,
    val province: String? = null,
    val country: String? = null,
    val phone: String? = null,
    val location: WsLocationDto? = null,
    val isActive: Boolean = true,
    val users: List<WsUserResponse> = emptyList(),
    val greenhouses: List<WsGreenhouseResponse> = emptyList(),
)

@Serializable
data class WsGreenhouseResponse(
    val id: Long,
    val code: String,
    val name: String,
    val location: WsLocationDto? = null,
    // Backend type is BigDecimal; serialised as a JSON number, identical to the REST
    // contract (`GreenhouseResponse.areaM2: Double?`). Keep `Double?` here so both paths
    // share the same decoded shape — precision is non-issue for greenhouse areas (a few
    // thousand m² at most).
    val areaM2: Double? = null,
    val timezone: String? = null,
    val isActive: Boolean = true,
    val sectors: List<WsSectorResponse> = emptyList(),
)

@Serializable
data class WsSectorResponse(
    val id: Long,
    val code: String,
    val name: String? = null,
    val devices: List<WsDeviceResponse> = emptyList(),
    val settings: List<WsSettingResponse> = emptyList(),
    val alerts: List<WsAlertResponse> = emptyList(),
)

@Serializable
data class WsDeviceResponse(
    val id: Long,
    val code: String,
    val name: String? = null,
    val clientName: String? = null,
    val isActive: Boolean = true,
    val category: WsDeviceCategoryDto? = null,
    val type: WsDeviceTypeDto? = null,
    val unit: WsUnitDto? = null,
    val currentValue: String? = null,
    val lastUpdated: String? = null,
)

@Serializable
data class WsSettingResponse(
    val id: Long,
    val code: String,
    val description: String? = null,
    val clientName: String? = null,
    val configuredValue: String? = null,
    val isActive: Boolean = true,
    val parameter: WsDeviceTypeDto? = null,
    val actuatorState: WsActuatorStateDto? = null,
    val dataType: WsDataTypeDto? = null,
    val currentValue: String? = null,
    val lastUpdated: String? = null,
)

@Serializable
data class WsAlertResponse(
    val id: Long,
    val code: String,
    val message: String? = null,
    val description: String? = null,
    val clientName: String? = null,
    val isResolved: Boolean = false,
    val resolvedAt: String? = null,
    val createdAt: String? = null,
    val alertType: WsAlertTypeDto? = null,
    val severity: WsAlertSeverityDto? = null,
    val resolvedByUser: WsUserResponse? = null,
)

// Catalog DTOs

@Serializable
data class WsLocationDto(
    val lat: Double? = null,
    val lon: Double? = null,
)

@Serializable
data class WsUserResponse(
    val id: Long,
    val code: String,
    val username: String,
    val email: String,
    val role: String,
    val isActive: Boolean = true,
    val lastLogin: String? = null,
)

@Serializable
data class WsDeviceCategoryDto(
    val id: Short,
    val name: String,
)

@Serializable
data class WsDeviceTypeDto(
    val id: Short,
    val name: String,
    val description: String? = null,
    val dataType: String? = null,
    val minExpectedValue: Double? = null,
    val maxExpectedValue: Double? = null,
    val controlType: String? = null,
    val categoryId: Short? = null,
)

@Serializable
data class WsUnitDto(
    val id: Short,
    val symbol: String,
    val name: String,
)

@Serializable
data class WsActuatorStateDto(
    val id: Short,
    val name: String,
    val description: String? = null,
    val isOperational: Boolean = false,
    val displayOrder: Short = 0,
    val color: String? = null,
)

@Serializable
data class WsDataTypeDto(
    val id: Short,
    val name: String,
    val description: String? = null,
    val validationRegex: String? = null,
    val exampleValue: String? = null,
)

@Serializable
data class WsAlertTypeDto(
    val id: Long,
    val name: String,
    val description: String? = null,
)

@Serializable
data class WsAlertSeverityDto(
    val id: Long,
    val name: String,
    val level: Int = 0,
    val description: String? = null,
    val color: String? = null,
    val requiresAction: Boolean = false,
    val notificationDelayMinutes: Int? = null,
)
