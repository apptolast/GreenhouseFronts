package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the greenhouse detail screen.
 */
data class GreenhouseDetailUiState(
    val isLoading: Boolean = true,
    val greenhouse: Greenhouse? = null,
    val error: String? = null,
    val isTogglingActive: Boolean = false,
)

/**
 * ViewModel for the greenhouse detail screen.
 *
 * @param greenhouseRepository Repository for greenhouse data
 */
class GreenhouseDetailViewModel(
    private val greenhouseRepository: GreenhouseRepository,
) : ViewModel() {

    val uiState: StateFlow<GreenhouseDetailUiState>
        field = MutableStateFlow(GreenhouseDetailUiState())

    fun loadGreenhouse(greenhouseId: Long) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            greenhouseRepository.getGreenhouseDetail(greenhouseId)
                .onSuccess { greenhouse ->
                    uiState.update { it.copy(isLoading = false, greenhouse = greenhouse) }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar invernadero",
                        )
                    }
                }
        }
    }

    fun toggleActive() {
        val greenhouse = uiState.value.greenhouse ?: return
        if (uiState.value.isTogglingActive) return

        val newActive = !greenhouse.isActive

        // Optimistic update
        uiState.update {
            it.copy(
                greenhouse = greenhouse.copy(isActive = newActive),
                isTogglingActive = true,
            )
        }

        viewModelScope.launch {
            greenhouseRepository.setGreenhouseActive(greenhouse.id, newActive)
                .onSuccess { updated ->
                    uiState.update {
                        it.copy(greenhouse = updated, isTogglingActive = false)
                    }
                }
                .onFailure { error ->
                    // Revert optimistic update
                    uiState.update {
                        it.copy(
                            greenhouse = greenhouse,
                            isTogglingActive = false,
                            error = error.message ?: "Error al actualizar estado",
                        )
                    }
                }
        }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }
}
