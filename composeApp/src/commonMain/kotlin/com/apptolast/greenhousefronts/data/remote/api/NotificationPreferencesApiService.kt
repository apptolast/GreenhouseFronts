package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.notification.UpdateUserNotificationPreferencesRequest
import com.apptolast.greenhousefronts.data.model.notification.UserNotificationPreferencesResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Per-user notification preferences endpoints. JWT-scoped: the backend resolves the
 * user from the bearer token, so no path/query params are needed.
 *
 *  - GET /api/v1/users/me/notification-preferences
 *  - PUT /api/v1/users/me/notification-preferences   (body = full preferences)
 *
 * The PUT requires all 7 fields — the caller must send the complete record, not a diff.
 */
class NotificationPreferencesApiService(
    private val httpClient: HttpClient,
) {
    suspend fun get(): UserNotificationPreferencesResponse =
        httpClient.get("$baseUrl/users/me/notification-preferences").body()

    suspend fun update(
        request: UpdateUserNotificationPreferencesRequest,
    ): UserNotificationPreferencesResponse =
        httpClient.put("$baseUrl/users/me/notification-preferences") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
