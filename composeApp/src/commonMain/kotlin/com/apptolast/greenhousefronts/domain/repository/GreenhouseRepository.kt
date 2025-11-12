package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage

interface GreenhouseRepository {
    suspend fun getRecentMessages(): Result<List<GreenhouseMessage>>
    suspend fun publishMessage(
        message: GreenhouseMessage,
        topic: String = "GREENHOUSE/RESPONSE",
        qos: Int = 0
    ): Result<String>
}