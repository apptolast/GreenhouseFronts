package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.notification.UserNotificationLogPageResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * In-app notification history. JWT-scoped: backend resolves the user from the bearer.
 *
 * - `GET /api/v1/users/me/notifications?cursor={int64?}&limit={1..100, default 50}`
 *
 * Cursor pagination: omit `cursor` for the first page; pass the `nextCursor` from the
 * previous response to fetch the next page. `hasMore = false` means terminal page.
 */
class NotificationLogApiService(
    private val httpClient: HttpClient,
) {
    suspend fun getNotifications(
        cursor: Long? = null,
        limit: Int = DEFAULT_LIMIT,
    ): UserNotificationLogPageResponse =
        httpClient.get("$baseUrl/users/me/notifications") {
            cursor?.let { parameter("cursor", it) }
            parameter("limit", limit)
        }.body()

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
