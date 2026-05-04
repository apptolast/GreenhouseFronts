package com.apptolast.greenhousefronts.data.model.notification

import kotlinx.serialization.Serializable

/**
 * GET /api/v1/users/me/notifications response. Cursor-paginated.
 *
 * The exact `items` schema is **not exposed by the live OpenAPI** at the time of writing.
 * Field defaults below are intentionally defensive — combined with the project-wide
 * `Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }`
 * config, the DTO tolerates the backend adding or renaming non-required fields without
 * crashing the client. Verify against a live response with curl before locking the schema.
 */
@Serializable
data class UserNotificationLogPageResponse(
    val items: List<UserNotificationLogItemResponse> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: Long? = null,
)

@Serializable
data class UserNotificationLogItemResponse(
    val id: Long,
    val alertId: Long? = null,
    val alertCode: String? = null,
    val title: String = "",
    val body: String? = null,
    val severityName: String? = null,
    val severityLevel: Short? = null,
    /** ISO-8601 UTC. Empty string default is a parse-failure sentinel for the formatter. */
    val sentAt: String = "",
    val channel: String? = null,
    val locale: String? = null,
    /**
     * FCM-style key/value extras. Same shape used by the backend's `AlertPushPayload`,
     * which the frontend's existing `AlertDeepLinkBus.emitFromData(...)` already knows
     * how to consume.
     */
    val deepLink: Map<String, String>? = null,
)
