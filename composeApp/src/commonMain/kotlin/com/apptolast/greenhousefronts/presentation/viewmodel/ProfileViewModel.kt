package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
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
            authRepository.logout()
            _events.send(ProfileEvent.LogoutSuccess)
        }
    }
}
