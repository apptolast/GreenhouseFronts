package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.remote.api.CommandApiService
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import com.apptolast.greenhousefronts.data.remote.websocket.WsGreenhouseResponse
import com.apptolast.greenhousefronts.data.remote.websocket.WsSectorResponse
import com.apptolast.greenhousefronts.domain.model.Device
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.domain.model.SectorWithDevices
import com.apptolast.greenhousefronts.domain.model.Setpoint
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the greenhouse detail screen.
 */
data class GreenhouseDetailUiState(
    val isLoading: Boolean = true,
    val greenhouse: Greenhouse? = null,
    val sectors: List<SectorWithDevices> = emptyList(),
    val selectedSectorIndex: Int = 0,
    val error: String? = null,
    val isTogglingActive: Boolean = false,
    val savingSetpointCodes: Set<String> = emptySet(),
)

/**
 * ViewModel for the greenhouse detail screen.
 * Loads basic greenhouse info via REST, then maintains a persistent
 * WebSocket connection for real-time device data updates.
 */
class GreenhouseDetailViewModel(
    private val greenhouseRepository: GreenhouseRepository,
    private val webSocket: GreenhouseStatusWebSocket,
    private val commandApiService: CommandApiService,
) : ViewModel() {

    val uiState: StateFlow<GreenhouseDetailUiState>
        field = MutableStateFlow(GreenhouseDetailUiState())

    private var wsJob: Job? = null

    fun loadGreenhouse(greenhouseId: Long) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            // Load greenhouse info via REST (for toggle active, basic info)
            greenhouseRepository.getGreenhouseDetail(greenhouseId)
                .onSuccess { greenhouse ->
                    uiState.update { it.copy(greenhouse = greenhouse) }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Error al cargar invernadero")
                    }
                    return@launch
                }

            // Start persistent WebSocket flow for real-time device data
            startDeviceUpdates(greenhouseId)
        }
    }

    /**
     * Starts collecting the WebSocket statusFlow for continuous updates.
     * Cancels any previous collection job first.
     */
    private fun startDeviceUpdates(greenhouseId: Long) {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            webSocket.statusFlow()
                .catch { e ->
                    println("[DETAIL-VM] WebSocket flow error: ${e.message}")
                    uiState.update { it.copy(isLoading = false) }
                }
                .collect { status ->
                    val wsGreenhouse = status.tenants
                        .flatMap { it.greenhouses }
                        .find { it.id == greenhouseId }

                    if (wsGreenhouse != null) {
                        val sectors = mapSectorsWithDevices(wsGreenhouse)
                        uiState.update {
                            it.copy(
                                isLoading = false,
                                sectors = sectors,
                                // Keep selected sector, but clamp if list changed
                                selectedSectorIndex = it.selectedSectorIndex.coerceIn(
                                    0,
                                    (sectors.size - 1).coerceAtLeast(0),
                                ),
                            )
                        }
                    } else {
                        uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    fun selectSector(index: Int) {
        uiState.update { it.copy(selectedSectorIndex = index) }
    }

    fun toggleActive() {
        val greenhouse = uiState.value.greenhouse ?: return
        if (uiState.value.isTogglingActive) return

        val newActive = !greenhouse.isActive

        uiState.update {
            it.copy(
                greenhouse = greenhouse.copy(isActive = newActive),
                isTogglingActive = true,
            )
        }

        viewModelScope.launch {
            greenhouseRepository.setGreenhouseActive(greenhouse.id, newActive)
                .onSuccess { updated ->
                    uiState.update { it.copy(greenhouse = updated, isTogglingActive = false) }
                }
                .onFailure { error ->
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

    /**
     * Sends a setpoint command to the PLC via the commands endpoint.
     * The backend validates the code, persists the command, and publishes to MQTT.
     * The PLC will update the value and the WebSocket will reflect it.
     */
    fun sendSetpointCommand(code: String, newValue: String) {
        if (uiState.value.savingSetpointCodes.contains(code)) return

        uiState.update { it.copy(savingSetpointCodes = it.savingSetpointCodes + code) }

        viewModelScope.launch {
            try {
                commandApiService.sendCommand(code, newValue)
            } catch (e: Exception) {
                uiState.update {
                    it.copy(error = e.message ?: "Error al enviar comando")
                }
            } finally {
                uiState.update { it.copy(savingSetpointCodes = it.savingSetpointCodes - code) }
            }
        }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
        println("[DETAIL-VM] ViewModel cleared, WebSocket job cancelled")
    }

    private fun mapSectorsWithDevices(wsGreenhouse: WsGreenhouseResponse): List<SectorWithDevices> {
        return wsGreenhouse.sectors
            .sortedBy { it.name }
            .map { sector ->
                SectorWithDevices(
                    id = sector.id,
                    code = sector.code,
                    name = sector.name ?: sector.code,
                    devices = mapDevices(sector),
                    setpoints = mapSetpoints(sector),
                )
            }
    }

    private fun mapDevices(sector: WsSectorResponse): List<Device> {
        return sector.devices
            .sortedBy { it.name }
            .map { device ->
                Device(
                    id = device.id,
                    code = device.code,
                    name = device.name ?: device.code,
                    clientName = device.clientName,
                    isActive = device.isActive,
                    categoryName = device.category?.name ?: "UNKNOWN",
                    typeName = device.type?.name ?: "UNKNOWN",
                    typeId = device.type?.id ?: 0,
                    unitSymbol = device.unit?.symbol?.takeIf { it != "-" },
                    currentValue = device.currentValue,
                    lastUpdated = device.lastUpdated,
                    minExpectedValue = device.type?.minExpectedValue,
                    maxExpectedValue = device.type?.maxExpectedValue,
                    controlType = device.type?.controlType,
                    dataType = device.type?.dataType,
                )
            }
    }

    private fun mapSetpoints(sector: WsSectorResponse): List<Setpoint> {
        return sector.settings
            .sortedBy { it.code }
            .map { setting ->
                Setpoint(
                    id = setting.id,
                    code = setting.code,
                    clientName = setting.clientName,
                    description = setting.description,
                    parameterName = setting.parameter?.name,
                    actuatorStateName = setting.actuatorState?.name,
                    dataTypeName = setting.dataType?.name,
                    currentValue = setting.currentValue,
                    configuredValue = setting.configuredValue,
                    isActive = setting.isActive,
                    lastUpdated = setting.lastUpdated,
                )
            }
    }
}
