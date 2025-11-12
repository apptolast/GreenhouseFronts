package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.remote.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class GreenhouseApiService {

    /**
     * Obtiene los mensajes recientes del invernadero
     * GET /api/greenhouse/messages/recent
     */
    suspend fun getRecentMessages(): List<GreenhouseMessage> {
        return KtorClient.httpClient.get("${KtorClient.baseUrl}/api/greenhouse/messages/recent").body()
    }

    /**
     * Publica un mensaje personalizado vía MQTT
     * POST /api/mqtt/publish/custom
     *
     * @param message El mensaje a publicar
     * @param topic El topic MQTT de destino (por defecto "GREENHOUSE/RESPONSE")
     * @param qos Quality of Service: 0, 1, o 2 (por defecto 0)
     */
    suspend fun publishMessage(
        message: GreenhouseMessage,
        topic: String = "GREENHOUSE/RESPONSE",
        qos: Int = 0
    ): String {
        return KtorClient.httpClient.post("${KtorClient.baseUrl}/api/mqtt/publish/custom") {
            parameter("topic", topic)
            parameter("qos", qos)
            contentType(ContentType.Application.Json)
            setBody(message)
        }.body()
    }
}