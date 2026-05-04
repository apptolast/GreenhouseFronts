package com.apptolast.greenhousefronts.domain.model

/**
 * One row in the user's in-app notification history.
 *
 * `severity` is null when the notification doesn't represent an alert (e.g. system or
 * subscription category). The screen falls back to a neutral leading dot in that case.
 */
data class NotificationLogEntry(
    val id: Long,
    val alertId: Long?,
    val alertCode: String?,
    val title: String,
    val body: String?,
    val severity: AlertSeverity?,
    /** Raw ISO-8601 — formatted in the UI via the shared `formatTimestamp` helper. */
    val sentAt: String,
    val channel: String?,
    /** FCM payload extras. When non-null and contains `alertId`, tap routes to Alerts. */
    val deepLink: Map<String, String>?,
)

/** Cursor-paginated page of notification log entries. */
data class NotificationLogPage(
    val items: List<NotificationLogEntry>,
    val hasMore: Boolean,
    val nextCursor: Long?,
)
