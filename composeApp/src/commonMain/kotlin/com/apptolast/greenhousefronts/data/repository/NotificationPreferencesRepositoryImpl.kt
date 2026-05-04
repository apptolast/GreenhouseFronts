package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.model.notification.UpdateUserNotificationPreferencesRequest
import com.apptolast.greenhousefronts.data.model.notification.UserNotificationPreferencesResponse
import com.apptolast.greenhousefronts.data.remote.api.NotificationPreferencesApiService
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.NotificationPreferences
import com.apptolast.greenhousefronts.domain.model.PreferredChannel
import com.apptolast.greenhousefronts.domain.model.QuietHours
import com.apptolast.greenhousefronts.domain.repository.NotificationPreferencesRepository

class NotificationPreferencesRepositoryImpl(
    private val api: NotificationPreferencesApiService,
) : NotificationPreferencesRepository {

    /**
     * Process-lifetime cache. Cleared implicitly on next-login because the GET right after
     * login overwrites it. If we ever observe stale-cache bugs across user switches we
     * can hook this up to AuthState.Unauthenticated.
     */
    private var cache: NotificationPreferences? = null

    override suspend fun getPreferences(forceRefresh: Boolean): Result<NotificationPreferences> =
        runCatching {
            if (!forceRefresh) {
                cache?.let { return@runCatching it }
            }
            val response = api.get()
            response.toDomain().also { cache = it }
        }

    override suspend fun updatePreferences(
        prefs: NotificationPreferences,
    ): Result<NotificationPreferences> = runCatching {
        val response = api.update(prefs.toRequest())
        response.toDomain().also { cache = it }
    }

    override fun cachedPreferences(): NotificationPreferences? = cache

    // --- Mappers ---

    private fun UserNotificationPreferencesResponse.toDomain(): NotificationPreferences {
        val severity = AlertSeverity.entries.firstOrNull { it.level.toInt() == minAlertSeverity }
            ?: AlertSeverity.INFO
        val quiet = if (quietHoursStart != null && quietHoursEnd != null) {
            QuietHours(start = quietHoursStart, end = quietHoursEnd)
        } else {
            null
        }
        return NotificationPreferences(
            categoryAlerts = categoryAlerts,
            categoryDevices = categoryDevices,
            categorySubscription = categorySubscription,
            minSeverity = severity,
            quietHours = quiet,
            timezone = quietHoursTimezone,
            channel = PreferredChannel.fromWire(preferredChannel),
            locale = locale,
        )
    }

    private fun NotificationPreferences.toRequest(): UpdateUserNotificationPreferencesRequest =
        UpdateUserNotificationPreferencesRequest(
            categoryAlerts = categoryAlerts,
            categoryDevices = categoryDevices,
            categorySubscription = categorySubscription,
            minAlertSeverity = minSeverity.level.toInt(),
            quietHoursStart = quietHours?.start,
            quietHoursEnd = quietHours?.end,
            quietHoursTimezone = timezone,
            preferredChannel = channel.name,
            locale = locale,
        )
}
