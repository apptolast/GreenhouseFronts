package com.apptolast.greenhousefronts.data.local.notification

import com.russhwolf.settings.Settings

/**
 * Persisted alert-notification preferences stored locally via multiplatform-settings.
 * Backed by SharedPreferences (Android) / NSUserDefaults (iOS) / java.util.prefs (JVM) /
 * localStorage (Web). Reads and writes are synchronous so they can be called from the
 * Android FCM service or the iOS notification delegate without a coroutine scope.
 */
class AlertNotificationSettings {

    private val settings: Settings = Settings()

    /** Whether to display alert push notifications. Default: true. */
    var alertsEnabled: Boolean
        get() = settings.getBoolean(KEY_ALERTS_ENABLED, true)
        set(value) = settings.putBoolean(KEY_ALERTS_ENABLED, value)

    /**
     * Minimum alert severity level to display.
     * Matches [com.apptolast.greenhousefronts.domain.model.AlertSeverity.level]:
     * 1 = INFO, 2 = WARNING, 3 = ERROR, 4 = CRITICAL. Default: 1 (show all).
     */
    var minSeverityLevel: Int
        get() = settings.getInt(KEY_MIN_SEVERITY, 1)
        set(value) = settings.putInt(KEY_MIN_SEVERITY, value)

    companion object {
        private const val KEY_ALERTS_ENABLED = "notification_alerts_enabled"
        private const val KEY_MIN_SEVERITY = "notification_min_severity"
    }
}
