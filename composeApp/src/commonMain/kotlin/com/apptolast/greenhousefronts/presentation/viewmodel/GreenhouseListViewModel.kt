package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the greenhouse list screen.
 */
data class GreenhouseListUiState(
    val isLoading: Boolean = true,
    val greenhouses: List<Greenhouse> = emptyList(),
    val displayName: String = "",
    val error: String? = null,
)

/**
 * ViewModel for the greenhouse list (home) screen.
 *
 * @param greenhouseRepository Repository for greenhouse data
 * @param authRepository Repository for user info (display name)
 */
class GreenhouseListViewModel(
    private val greenhouseRepository: GreenhouseRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<GreenhouseListUiState>
        field = MutableStateFlow(GreenhouseListUiState())

    init {
        loadDisplayName()
        loadGreenhouses()
    }

    fun loadGreenhouses() {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            greenhouseRepository.getGreenhouses()
                .onSuccess { greenhouses ->
                    uiState.update {
                        it.copy(isLoading = false, greenhouses = greenhouses)
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar invernaderos",
                        )
                    }
                }
        }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    private fun loadDisplayName() {
        viewModelScope.launch {
            val name = authRepository.getDisplayName()
                ?: authRepository.getUsername()?.substringBefore("@")
                ?: ""
            uiState.update { it.copy(displayName = name) }
        }
    }
}
