package com.apptolast.greenhousefronts.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the alert id that the user tapped on a push notification. The Alerts screen reads
 * this once when it opens and clears it via [consume] so a screen rotation doesn't replay
 * the deep-link selection.
 *
 * Lives outside the ViewModel because the deep-link arrives before the screen exists (cold
 * start) and may also need to survive across login (notification tapped while logged out).
 */
class PendingAlertSelectionBus {
    private val _state = MutableStateFlow<Long?>(null)
    val state: StateFlow<Long?> = _state.asStateFlow()

    fun set(alertId: Long?) {
        _state.value = alertId
    }

    /** Returns the pending alert id (if any) and clears it atomically. */
    fun consume(): Long? {
        val current = _state.value
        if (current != null) _state.value = null
        return current
    }
}
