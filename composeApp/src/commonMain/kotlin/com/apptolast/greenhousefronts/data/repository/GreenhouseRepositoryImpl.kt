package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.remote.api.GreenhouseApiService
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository

class GreenhouseRepositoryImpl(
    private val apiService: GreenhouseApiService = GreenhouseApiService()
) : GreenhouseRepository {

    override suspend fun getRecentMessages(): Result<List<GreenhouseMessage>> {
        return try {
            val messages = apiService.getRecentMessages()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun publishMessage(
        message: GreenhouseMessage,
        topic: String,
        qos: Int
    ): Result<String> {
        return try {
            val response = apiService.publishMessage(message, topic, qos)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}