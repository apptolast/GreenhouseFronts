package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.remote.push.PushTokenRegistrar
import com.apptolast.greenhousefronts.domain.model.UserProfile
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the profile screen.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val error: String? = null,
    val isLoggingOut: Boolean = false,
)

/**
 * One-time events for navigation.
 */
sealed interface ProfileEvent {
    data object LogoutSuccess : ProfileEvent
}

/**
 * ViewModel for the profile screen.
 *
 * @param userRepository Repository for user profile data
 * @param authRepository Repository for auth operations (logout)
 * @param pushTokenRegistrar Used to drop the FCM token from the backend BEFORE the JWT
 *   is wiped — same contract as `AuthViewModel.logout()`. Without this the backend
 *   would keep pushing notifications to a "ghost" device until the next login.
 */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState>
        field = MutableStateFlow(ProfileUiState())

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
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

    fun logout() {
        if (uiState.value.isLoggingOut) return

        viewModelScope.launch {
            uiState.update { it.copy(isLoggingOut = true) }
            // Drop the FCM token from the backend BEFORE the JWT is wiped, otherwise
            // the DELETE call would fail authentication. Same order as in
            // `AuthViewModel.logout()`.
            pushTokenRegistrar.unregisterCurrentToken()
            authRepository.logout()
            _events.send(ProfileEvent.LogoutSuccess)
        }
    }
}
