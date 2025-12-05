package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the sensor detail screen
 */
data class SensorDetailUiState(
    val isLoading: Boolean = true,
    val statistics: SensorStatistics? = null,
    val error: String? = null,
    val selectedPeriod: TimePeriod = TimePeriod.LAST_24H
)

/**
 * ViewModel for sensor detail operations
 * Uses constructor injection for repository (provided by Koin)
 *
 * @param repository Injected repository for data operations
 */
class SensorDetailViewModel(
    private val repository: GreenhouseRepository
) : ViewModel() {

    val uiState: StateFlow<SensorDetailUiState>
        field = MutableStateFlow(SensorDetailUiState())

    private var currentGreenhouseId: String? = null
    private var currentSensorType: SensorType? = null

    /**
     * Initializes the screen with greenhouse and sensor data
     */
    fun initialize(
        greenhouseId: String,
        sensorType: SensorType
    ) {
        if (currentGreenhouseId != greenhouseId || currentSensorType != sensorType) {
            currentGreenhouseId = greenhouseId
            currentSensorType = sensorType
            loadStatistics()
        }
    }

    /**
     * Loads statistics for the current sensor and period
     */
    fun loadStatistics() {
        val greenhouseId = currentGreenhouseId ?: return
        val sensorType = currentSensorType ?: return
        val period = uiState.value.selectedPeriod

        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true, error = null)

            repository.getStatistics(
                greenhouseId = greenhouseId,
                sensorType = sensorType,
                period = period
            )
                .onSuccess { statistics ->
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        statistics = statistics,
                        error = null
                    )
                }
                .onFailure { exception ->
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    /**
     * Changes the selected time period and reloads data
     */
    fun changePeriod(period: TimePeriod) {
        if (uiState.value.selectedPeriod != period) {
            uiState.value = uiState.value.copy(selectedPeriod = period)
            loadStatistics()
        }
    }

    /**
     * Retries loading statistics after an error
     */
    fun retry() {
        loadStatistics()
    }

    /**
     * Clears any error messages
     */
    fun clearError() {
        uiState.value = uiState.value.copy(error = null)
    }
}
