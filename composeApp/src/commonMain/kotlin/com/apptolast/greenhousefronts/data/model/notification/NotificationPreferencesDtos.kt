package com.apptolast.greenhousefronts.data.model.notification

import kotlinx.serialization.Serializable

/**
 * GET /api/v1/users/me/notification-preferences response.
 *
 * Backend serves this from the per-user `user_notification_preferences` row, joined with
 * the user's `locale` field. All 7 fields are present on a populated record; `quietHours*`
 * times are nullable when the user disabled the quiet-hours feature.
 */
@Serializable
data class UserNotificationPreferencesResponse(
    val categoryAlerts: Boolean,
    val categoryDevices: Boolean,
    val categorySubscription: Boolean,
    val minAlertSeverity: Int,
    /** "HH:mm" — null if quiet hours are disabled. */
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    /** Required IANA tz id (e.g. "Europe/Madrid"). */
    val quietHoursTimezone: String,
    /** "PUSH" / "IN_APP" / "EMAIL" — free-form on the wire, validated at the domain layer. */
    val preferredChannel: String,
    /** BCP-47 (e.g. "es-ES"). */
    val locale: String,
)

/**
 * PUT /api/v1/users/me/notification-preferences body.
 *
 * **All 7 required fields must be sent on every PUT** — the backend does not accept
 * partial updates. The screen always holds the complete object loaded from the GET, so
 * the request is built straight from that domain object.
 */
@Serializable
data class UpdateUserNotificationPreferencesRequest(
    val categoryAlerts: Boolean,
    val categoryDevices: Boolean,
    val categorySubscription: Boolean,
    val minAlertSeverity: Int,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val quietHoursTimezone: String,
    val preferredChannel: String,
    val locale: String,
)
