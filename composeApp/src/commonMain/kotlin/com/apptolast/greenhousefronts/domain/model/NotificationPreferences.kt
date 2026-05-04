package com.apptolast.greenhousefronts.domain.model

/**
 * User-scoped notification preferences. Mapped 1:1 from
 * `UserNotificationPreferencesResponse`. The screen edits a `copy()` of this object;
 * `save()` ships the whole thing back via PUT (no partial updates supported).
 */
data class NotificationPreferences(
    val categoryAlerts: Boolean,
    val categoryDevices: Boolean,
    val categorySubscription: Boolean,
    /** Reused enum so the UI can render severity chips consistently with Alerts. */
    val minSeverity: AlertSeverity,
    /** Null when the user disabled quiet hours. */
    val quietHours: QuietHours?,
    /** IANA tz id; defaults to system tz on first edit if blank. */
    val timezone: String,
    val channel: PreferredChannel,
    /** BCP-47, e.g. "es-ES". */
    val locale: String,
)

data class QuietHours(
    /** "HH:mm" — 24-hour, e.g. "22:00". */
    val start: String,
    val end: String,
)

enum class PreferredChannel {
    PUSH,
    IN_APP,
    EMAIL;

    companion object {
        /** Defensive: backend is documented as "free-form string"; fall back to PUSH. */
        fun fromWire(raw: String): PreferredChannel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PUSH
    }
}
