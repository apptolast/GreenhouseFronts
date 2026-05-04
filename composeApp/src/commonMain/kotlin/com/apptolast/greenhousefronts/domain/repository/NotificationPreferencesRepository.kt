package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.NotificationPreferences

/**
 * Per-user notification settings. Backed by `/api/v1/users/me/notification-preferences`.
 *
 * The repository keeps an in-memory cache of the last fetched record so the screen can
 * render instantly on re-entry; [getPreferences] honors `forceRefresh` to bypass it.
 */
interface NotificationPreferencesRepository {
    suspend fun getPreferences(forceRefresh: Boolean = false): Result<NotificationPreferences>

    /**
     * Writes the full preferences object via PUT. The backend requires every field on
     * every request — the screen always passes the complete object, mutated via `copy()`
     * from the loaded GET response.
     */
    suspend fun updatePreferences(prefs: NotificationPreferences): Result<NotificationPreferences>

    /** Last-fetched value, or null if no GET has succeeded yet this process. */
    fun cachedPreferences(): NotificationPreferences?
}
