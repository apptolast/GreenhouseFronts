package com.apptolast.greenhousefronts.data.local.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persisted preferences for global visual effects, exposed as reactive state so
 * composables mounted at the App root (e.g., the critical-alert heartbeat) can
 * react instantly when the user toggles them from the profile screen — without
 * having to re-read multiplatform-settings on every recomposition.
 *
 * Multiplatform-settings is the source of truth on disk; the in-memory
 * [MutableStateFlow] is a write-through cache initialised from disk.
 */
class VisualEffectsSettings {

    private val settings: Settings = Settings()

    private val _heartbeatEnabled = MutableStateFlow(
        settings.getBoolean(KEY_HEARTBEAT_ENABLED, DEFAULT_HEARTBEAT_ENABLED),
    )

    /** Whether the pulsing red border is shown when there are unresolved CRITICAL alerts. */
    val heartbeatEnabled: StateFlow<Boolean> = _heartbeatEnabled.asStateFlow()

    fun setHeartbeatEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_HEARTBEAT_ENABLED, enabled)
        _heartbeatEnabled.value = enabled
    }

    companion object {
        private const val KEY_HEARTBEAT_ENABLED = "visual_effects_heartbeat_enabled"
        private const val DEFAULT_HEARTBEAT_ENABLED = true
    }
}
