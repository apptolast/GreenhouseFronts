package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.remote.baseUrl
import com.apptolast.greenhousefronts.data.remote.push.PushPlatform
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import kotlinx.serialization.Serializable

/**
 * Registers and unregisters FCM tokens with the backend.
 *
 * Endpoints (see `apptolast/InvernaderosAPI`):
 *  - `POST   /api/v1/push-tokens`        — upsert the token for the JWT-resolved user
 *  - `DELETE /api/v1/push-tokens/{token}` — drop the token (called on logout)
 *
 * The backend resolves `userId` and `tenantId` from the JWT, so the request body only
 * carries the FCM token and the platform identifier.
 */
class PushTokenApiService(
    private val httpClient: HttpClient,
) {
    suspend fun register(token: String, platform: PushPlatform) {
        httpClient.post("$baseUrl/push-tokens") {
            contentType(ContentType.Application.Json)
            setBody(RegisterPushTokenRequest(token = token, platform = platform.name))
        }
    }

    suspend fun unregister(token: String) {
        httpClient.delete("$baseUrl/push-tokens/${token.encodeURLPath()}")
    }
}

@Serializable
data class RegisterPushTokenRequest(
    val token: String,
    val platform: String,
)
