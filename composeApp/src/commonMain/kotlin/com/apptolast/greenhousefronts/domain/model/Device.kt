package com.apptolast.greenhousefronts.domain.model

/**
 * Domain model representing a device (sensor or actuator) with its real-time value.
 */
data class Device(
    val id: Long,
    val code: String,
    val name: String,
    val clientName: String? = null,
    val isActive: Boolean,
    val categoryName: String,
    val typeName: String,
    val typeId: Short,
    val unitSymbol: String?,
    val currentValue: String?,
    val lastUpdated: String?,
    val minExpectedValue: Double?,
    val maxExpectedValue: Double?,
    val controlType: String?,
)

/**
 * Domain model representing a setpoint (consigna) configured for a sector.
 */
data class Setpoint(
    val id: Long,
    val code: String,
    val clientName: String? = null,
    val description: String?,
    val parameterName: String?,
    val actuatorStateName: String?,
    val dataTypeName: String?,
    val currentValue: String?,
    val configuredValue: String?,
    val isActive: Boolean,
    val lastUpdated: String?,
)

/**
 * Domain model representing a sector with its devices for the detail screen.
 */
data class SectorWithDevices(
    val id: Long,
    val code: String,
    val name: String,
    val devices: List<Device>,
    val setpoints: List<Setpoint> = emptyList(),
)
