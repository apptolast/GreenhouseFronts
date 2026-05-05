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
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailViewModel.Companion.SETPOINT_CONFIRM_TIMEOUT_MS
import com.apptolast.greenhousefronts.util.isFalseLike
import com.apptolast.greenhousefronts.util.isTrueLike
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 *
 * The screen reflects the WebSocket snapshot as the single source of truth — no initial
 * REST fetch is performed. The first STOMP frame populates the greenhouse, its sectors,
 * devices, setpoints and alert count in one shot. The toggle-active action fires only a
 * PUT and lets the backend's `GREENHOUSE_CRUD` push update the UI.
 */
class GreenhouseDetailViewModel(
    private val greenhouseRepository: GreenhouseRepository,
    private val webSocket: GreenhouseStatusWebSocket,
    private val commandApiService: CommandApiService,
) : ViewModel() {

    val uiState: StateFlow<GreenhouseDetailUiState>
        field = MutableStateFlow(GreenhouseDetailUiState())

    private var wsJob: Job? = null

    /**
     * Setpoints awaiting WebSocket confirmation after a sendCommand.
     * The spinner stays visible until either:
     *   - the WS pushes a status where this setpoint's `currentValue` matches `expectedValue` (success)
     *   - [SETPOINT_CONFIRM_TIMEOUT_MS] elapses (timeout → user-facing error)
     *   - the REST POST itself fails (immediate clear)
     */
    private data class PendingSetpoint(val expectedValue: String, val timeoutJob: Job)

    private val pendingSetpoints = mutableMapOf<String, PendingSetpoint>()

    fun loadGreenhouse(greenhouseId: Long) {
        uiState.update { it.copy(isLoading = true, error = null) }
        startDeviceUpdates(greenhouseId)
    }

    /**
     * Collects the WebSocket statusFlow and projects it to [GreenhouseDetailUiState].
     * Cancels any previous collection job first so re-entering the screen with a
     * different greenhouseId does not leave a stale collector running.
     *
     * The user-driven `isTogglingActive` flag is preserved across emissions so the
     * switch keeps showing the spinner while the PUT is in flight, even if a WS
     * push arrives meanwhile.
     */
    private fun startDeviceUpdates(greenhouseId: Long) {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            webSocket.statusFlow()
                .catch { e ->
                    println("[DETAIL-VM] WebSocket flow error: ${e::class.simpleName}: ${e.message}")
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se pudo conectar al servidor en tiempo real. Reintentando…",
                        )
                    }
                }
                .collect { status ->
                    val wsGreenhouse = status.tenants
                        .flatMap { it.greenhouses }
                        .find { it.id == greenhouseId }

                    if (wsGreenhouse == null) {
                        // The current snapshot doesn't include this greenhouseId. The first
                        // arriving snapshot for a new connection might not carry it yet
                        // (replay = 1 may surface the previous detail's snapshot); skip
                        // silently while loading. If we already had data and it disappeared,
                        // surface an error so the screen doesn't go blank without feedback.
                        if (uiState.value.greenhouse != null) {
                            uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Este invernadero ya no está disponible para tu cuenta.",
                                )
                            }
                        }
                        return@collect
                    }

                    val sectors = mapSectorsWithDevices(wsGreenhouse)
                    reconcilePendingSetpoints(sectors)
                    val activeAlertCount = wsGreenhouse.sectors
                        .sumOf { sector -> sector.alerts.count { !it.isResolved } }

                    uiState.update { current ->
                        // Build the domain Greenhouse from WS data. Preserve the local
                        // optimistic `isActive` while a toggle PUT is in flight — the
                        // server push that confirms the change will overwrite it once
                        // `isTogglingActive` clears.
                        val previous = current.greenhouse
                        val merged = mapGreenhouse(wsGreenhouse, sectors, activeAlertCount).let {
                            if (current.isTogglingActive && previous != null) {
                                it.copy(isActive = previous.isActive)
                            } else {
                                it
                            }
                        }

                        current.copy(
                            isLoading = false,
                            error = null,
                            greenhouse = merged,
                            sectors = sectors,
                            savingSetpointCodes = pendingSetpoints.keys.toSet(),
                            // Keep selected sector, but clamp if list changed
                            selectedSectorIndex = current.selectedSectorIndex
                                .coerceIn(0, (sectors.size - 1).coerceAtLeast(0)),
                        )
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

        // Optimistic update — the WS push will confirm (and overwrite) when it arrives.
        uiState.update {
            it.copy(
                greenhouse = greenhouse.copy(isActive = newActive),
                isTogglingActive = true,
            )
        }

        viewModelScope.launch {
            greenhouseRepository.setGreenhouseActive(greenhouse.id, newActive)
                .onSuccess {
                    uiState.update { it.copy(isTogglingActive = false) }
                }
                .onFailure { error ->
                    // Revert optimistic update on failure.
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
     *
     * The spinner is kept visible until the WebSocket confirms the new `currentValue`
     * (typically 1–2 s) or [SETPOINT_CONFIRM_TIMEOUT_MS] elapses. The REST POST
     * completing successfully is NOT enough to clear the spinner — only the WS
     * round-trip proves the value reached the PLC.
     */
    fun sendSetpointCommand(code: String, newValue: String) {
        if (pendingSetpoints.containsKey(code)) return

        val timeoutJob = viewModelScope.launch {
            delay(SETPOINT_CONFIRM_TIMEOUT_MS)
            resolveSetpointTimeout(code)
        }
        pendingSetpoints[code] = PendingSetpoint(newValue, timeoutJob)
        uiState.update { it.copy(savingSetpointCodes = pendingSetpoints.keys.toSet()) }

        viewModelScope.launch {
            try {
                commandApiService.sendCommand(code, newValue)
                // Do not clear pending here: wait for WS reconciliation or timeout.
            } catch (e: Exception) {
                pendingSetpoints.remove(code)?.timeoutJob?.cancel()
                uiState.update {
                    it.copy(
                        savingSetpointCodes = pendingSetpoints.keys.toSet(),
                        error = e.message ?: "Error al enviar comando",
                    )
                }
            }
        }
    }

    private fun resolveSetpointTimeout(code: String) {
        if (pendingSetpoints.remove(code) == null) return
        uiState.update {
            it.copy(
                savingSetpointCodes = pendingSetpoints.keys.toSet(),
                error = "No se pudo confirmar la actualización de $code (10 s).",
            )
        }
    }

    /**
     * For each pending setpoint, checks whether the WS-reported `currentValue`
     * matches the value the user sent. If so, cancels the timeout and clears
     * the pending state so the spinner disappears.
     */
    private fun reconcilePendingSetpoints(sectors: List<SectorWithDevices>) {
        if (pendingSetpoints.isEmpty()) return
        val confirmed = mutableListOf<String>()
        for ((code, pending) in pendingSetpoints) {
            val setpoint = sectors.firstNotNullOfOrNull { sector ->
                sector.setpoints.firstOrNull { it.code == code }
            } ?: continue
            if (matchesExpected(setpoint.currentValue, pending.expectedValue, setpoint.dataTypeName)) {
                pending.timeoutJob.cancel()
                confirmed += code
            }
        }
        confirmed.forEach { pendingSetpoints.remove(it) }
    }

    private fun matchesExpected(current: String?, expected: String, dataType: String?): Boolean {
        current ?: return false
        if (dataType.equals("BOOLEAN", ignoreCase = true)) {
            return (expected.isTrueLike() && current.isTrueLike()) ||
                    (expected.isFalseLike() && current.isFalseLike())
        }
        val curNum = current.toDoubleOrNull()
        val expNum = expected.toDoubleOrNull()
        if (curNum != null && expNum != null) return curNum == expNum
        return current == expected
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
        pendingSetpoints.values.forEach { it.timeoutJob.cancel() }
        pendingSetpoints.clear()
        println("[DETAIL-VM] ViewModel cleared, WebSocket job cancelled")
    }

    companion object {
        private const val SETPOINT_CONFIRM_TIMEOUT_MS = 10_000L
    }

    private fun mapGreenhouse(
        wsGreenhouse: WsGreenhouseResponse,
        sectors: List<SectorWithDevices>,
        activeAlertCount: Int,
    ): Greenhouse {
        return Greenhouse(
            id = wsGreenhouse.id,
            code = wsGreenhouse.code,
            name = wsGreenhouse.name,
            isActive = wsGreenhouse.isActive,
            areaM2 = wsGreenhouse.areaM2,
            sectorCount = sectors.size,
            alertCount = activeAlertCount,
            sectorNames = sectors.map { it.name }.sorted(),
        )
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
