package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val logoutSuccess: Boolean = false
)

/**
 * ViewModel for the Settings screen.
 * Handles logout and other settings operations.
 */
class SettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState>
        field = MutableStateFlow(SettingsUiState())

    /**
     * Logs out the current user.
     */
    fun logout() {
        if (uiState.value.isLoading) return

        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            try {
                authRepository.logout()
                uiState.update { it.copy(isLoading = false, logoutSuccess = true) }
            } catch (e: Exception) {
                uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cerrar sesión"
                    )
                }
            }
        }
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        uiState.update { it.copy(error = null) }
    }
}
