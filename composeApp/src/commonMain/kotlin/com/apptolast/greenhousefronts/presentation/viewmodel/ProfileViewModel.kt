package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.local.notification.AlertNotificationSettings
import com.apptolast.greenhousefronts.data.local.settings.VisualEffectsSettings
import com.apptolast.greenhousefronts.data.remote.push.PushTokenRegistrar
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.UserProfile
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val error: String? = null,
    val isLoggingOut: Boolean = false,
    val alertsEnabled: Boolean = true,
    val minSeverity: AlertSeverity = AlertSeverity.INFO,
    val heartbeatEnabled: Boolean = true,
)

sealed interface ProfileEvent {
    data object LogoutSuccess : ProfileEvent
}

/**
 * ViewModel for the profile screen.
 *
 * Owns the two local alert-notification preferences (alertsEnabled + minSeverity) that are
 * now displayed inline on this screen. Persists them via [AlertNotificationSettings] so
 * they survive process death and are readable by the Android FCM service and iOS delegate.
 */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val visualEffectsSettings: VisualEffectsSettings,
) : ViewModel() {

    private val alertSettings = AlertNotificationSettings()

    val uiState: StateFlow<ProfileUiState>
        field = MutableStateFlow(
            ProfileUiState(
                alertsEnabled = alertSettings.alertsEnabled,
                minSeverity = AlertSeverity.entries.firstOrNull {
                    it.level.toInt() == alertSettings.minSeverityLevel
                } ?: AlertSeverity.INFO,
                heartbeatEnabled = visualEffectsSettings.heartbeatEnabled.value,
            ),
        )

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
        // Mirror the persisted heartbeat flag into UiState so the screen reflects
        // changes that originate elsewhere too (e.g., a future deep-link toggle).
        viewModelScope.launch {
            visualEffectsSettings.heartbeatEnabled.collect { enabled ->
                uiState.update { it.copy(heartbeatEnabled = enabled) }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }
            userRepository.getCurrentUserProfile()
                .onSuccess { profile ->
                    uiState.update { it.copy(isLoading = false, profile = profile) }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar perfil",
                        )
                    }
                }
        }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        alertSettings.alertsEnabled = enabled
        uiState.update { it.copy(alertsEnabled = enabled) }
    }

    fun setMinSeverity(severity: AlertSeverity) {
        alertSettings.minSeverityLevel = severity.level.toInt()
        uiState.update { it.copy(minSeverity = severity) }
    }

    fun setHeartbeatEnabled(enabled: Boolean) {
        // Single write-through: VisualEffectsSettings persists and emits on its
        // StateFlow; the collector above pulls the new value into uiState.
        visualEffectsSettings.setHeartbeatEnabled(enabled)
    }

    fun logout() {
        if (uiState.value.isLoggingOut) return
        viewModelScope.launch {
            uiState.update { it.copy(isLoggingOut = true) }
            // Drop the FCM token from the backend BEFORE the JWT is wiped, otherwise
            // the DELETE call would fail authentication.
            pushTokenRegistrar.unregisterCurrentToken()
            authRepository.logout()
            _events.send(ProfileEvent.LogoutSuccess)
        }
    }
}
