package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.model.notification.UserNotificationLogItemResponse
import com.apptolast.greenhousefronts.data.remote.api.NotificationLogApiService
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.NotificationLogEntry
import com.apptolast.greenhousefronts.domain.model.NotificationLogPage
import com.apptolast.greenhousefronts.domain.repository.NotificationLogRepository

class NotificationLogRepositoryImpl(
    private val api: NotificationLogApiService,
) : NotificationLogRepository {

    override suspend fun getPage(
        cursor: Long?,
        limit: Int,
    ): Result<NotificationLogPage> = runCatching {
        val response = api.getNotifications(cursor = cursor, limit = limit)
        NotificationLogPage(
            items = response.items.map { it.toDomain() },
            hasMore = response.hasMore,
            nextCursor = response.nextCursor,
        )
    }

    private fun UserNotificationLogItemResponse.toDomain(): NotificationLogEntry =
        NotificationLogEntry(
            id = id,
            alertId = alertId,
            alertCode = alertCode,
            title = title,
            body = body,
            severity = AlertSeverity.fromName(severityName),
            sentAt = sentAt,
            channel = channel,
            deepLink = deepLink,
        )
}
