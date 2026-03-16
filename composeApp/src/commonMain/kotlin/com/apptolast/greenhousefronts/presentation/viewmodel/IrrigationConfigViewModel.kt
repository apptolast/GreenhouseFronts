package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.model.DayOfWeek
import com.apptolast.greenhousefronts.domain.model.IrrigationConfig
import com.apptolast.greenhousefronts.domain.model.SectorIrrigationConfig
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the irrigation configuration screen.
 */
data class IrrigationConfigUiState(
    val isLoading: Boolean = true,
    val config: IrrigationConfig? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
)

/**
 * ViewModel for the irrigation configuration screen.
 * Loads sector data from the greenhouse and manages local form state.
 *
 * @param greenhouseRepository Repository for greenhouse/sector data
 */
class IrrigationConfigViewModel(
    private val greenhouseRepository: GreenhouseRepository,
) : ViewModel() {

    val uiState: StateFlow<IrrigationConfigUiState>
        field = MutableStateFlow(IrrigationConfigUiState())

    fun loadConfig(greenhouseId: Long) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            greenhouseRepository.getGreenhouseDetail(greenhouseId)
                .onSuccess { greenhouse ->
                    val sectorConfigs = greenhouse.sectorNames.mapIndexed { index, name ->
                        SectorIrrigationConfig(
                            sectorId = index.toLong(),
                            sectorName = name,
                        )
                    }
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            config = IrrigationConfig(
                                greenhouseId = greenhouse.id,
                                greenhouseName = greenhouse.name,
                                sectorConfigs = sectorConfigs,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar configuración",
                        )
                    }
                }
        }
    }

    fun toggleDay(day: DayOfWeek) {
        val config = uiState.value.config ?: return
        val newDays = config.activeDays.toMutableSet()
        if (day in newDays) newDays.remove(day) else newDays.add(day)
        uiState.update { it.copy(config = config.copy(activeDays = newDays)) }
    }

    fun updateStartHour(value: Int) {
        val config = uiState.value.config ?: return
        uiState.update { it.copy(config = config.copy(startHour = value.coerceIn(0, 23))) }
    }

    fun updateStartMinute(value: Int) {
        val config = uiState.value.config ?: return
        uiState.update { it.copy(config = config.copy(startMinute = value.coerceIn(0, 59))) }
    }

    fun updateEndHour(value: Int) {
        val config = uiState.value.config ?: return
        uiState.update { it.copy(config = config.copy(endHour = value.coerceIn(0, 23))) }
    }

    fun updateEndMinute(value: Int) {
        val config = uiState.value.config ?: return
        uiState.update { it.copy(config = config.copy(endMinute = value.coerceIn(0, 59))) }
    }

    fun updateWaitBetween(value: Int) {
        val config = uiState.value.config ?: return
        uiState.update { it.copy(config = config.copy(waitBetweenMinutes = value.coerceAtLeast(0))) }
    }

    fun updateSectorOpening(sectorIndex: Int, minutes: Int) {
        val config = uiState.value.config ?: return
        val updated = config.sectorConfigs.toMutableList()
        if (sectorIndex in updated.indices) {
            updated[sectorIndex] = updated[sectorIndex].copy(openingMinutes = minutes.coerceAtLeast(0))
            uiState.update { it.copy(config = config.copy(sectorConfigs = updated)) }
        }
    }

    fun updateSectorWait(sectorIndex: Int, minutes: Int) {
        val config = uiState.value.config ?: return
        val updated = config.sectorConfigs.toMutableList()
        if (sectorIndex in updated.indices) {
            updated[sectorIndex] = updated[sectorIndex].copy(waitMinutes = minutes.coerceAtLeast(0))
            uiState.update { it.copy(config = config.copy(sectorConfigs = updated)) }
        }
    }

    fun toggleSectorActive(sectorIndex: Int) {
        val config = uiState.value.config ?: return
        val updated = config.sectorConfigs.toMutableList()
        if (sectorIndex in updated.indices) {
            updated[sectorIndex] = updated[sectorIndex].copy(isActive = !updated[sectorIndex].isActive)
            uiState.update { it.copy(config = config.copy(sectorConfigs = updated)) }
        }
    }

    fun saveConfig() {
        val config = uiState.value.config ?: return
        if (uiState.value.isSaving) return

        viewModelScope.launch {
            uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

            // TODO: Send configuration to backend Settings API when endpoint is ready
            // For now, simulate save success
            kotlinx.coroutines.delay(500)
            uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun clearSaveSuccess() {
        uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }
}
