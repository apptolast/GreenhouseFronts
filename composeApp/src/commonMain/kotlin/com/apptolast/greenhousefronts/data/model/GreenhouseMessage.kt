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
)