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
 *
 * `isLoading` is for the initial fetch (covers the screen with a progress bar);
 * `isRefreshing` is the user-driven pull-to-refresh state (drives the M3 indicator
 * without blocking the visible list).
 */
data class GreenhouseListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
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

    /**
     * Initial fetch — drives [GreenhouseListUiState.isLoading] and a full-screen spinner.
     * Called from `init` and from `LifecycleResumeEffect` whenever the tab regains focus.
     */
    fun loadGreenhouses() {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }
            fetch()
            uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * User-initiated pull-to-refresh — drives [GreenhouseListUiState.isRefreshing].
     * The visible list stays on screen while the indicator spins.
     */
    fun refresh() {
        if (uiState.value.isRefreshing) return
        viewModelScope.launch {
            uiState.update { it.copy(isRefreshing = true, error = null) }
            fetch()
            uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Single fetch routine shared by [loadGreenhouses] and [refresh]. Updates the list and
     * the error string but leaves the loading/refreshing flags to the caller, so each entry
     * point can drive its own UI semantics.
     */
    private suspend fun fetch() {
        greenhouseRepository.getGreenhouses()
            .onSuccess { greenhouses ->
                uiState.update { it.copy(greenhouses = greenhouses, error = null) }
            }
            .onFailure { error ->
                uiState.update { it.copy(error = error.message ?: "Error al cargar invernaderos") }
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
