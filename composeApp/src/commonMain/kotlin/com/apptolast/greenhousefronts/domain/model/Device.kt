package com.apptolast.greenhousefronts.domain.model

/**
 * Domain model representing a device (sensor or actuator) with its real-time value.
 */
data class Device(
    val id: Long,
    val code: String,
    val name: String,
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
 * Domain model representing a sector with its devices for the detail screen.
 */
data class SectorWithDevices(
    val id: Long,
    val code: String,
    val name: String,
    val devices: List<Device>,
)
