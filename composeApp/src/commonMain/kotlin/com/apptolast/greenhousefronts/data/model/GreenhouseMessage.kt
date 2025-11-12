package com.apptolast.greenhousefronts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GreenhouseMessage(
    val timestamp: String,
    val sensor01: Double? = null,
    val sensor02: Double? = null,
    val setpoint01: Double? = null,
    val setpoint02: Double? = null,
    val setpoint03: Double? = null,
    val greenhouseId: String,
    val rawPayload: String? = null,
    val sensors: Object1? = null,
    val setpoints: Object2? = null,
)

@Serializable
data class Object1(
    val SENSOR_01: Double? = null,
    val SENSOR_02: Double? = null
)

@Serializable
data class Object2(
    val SETPOINT_01: Double? = null,
    val SETPOINT_02: Double? = null,
    val SETPOINT_03: Double? = null
)

