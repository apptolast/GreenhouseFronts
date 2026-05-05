package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.remote.api.CommandApiService
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import com.apptolast.greenhousefronts.data.remote.websocket.WsSectorResponse
import com.apptolast.greenhousefronts.data.remote.websocket.WsSettingResponse
import com.apptolast.greenhousefronts.util.isTrueLike
import com.apptolast.greenhousefronts.domain.model.DayOfWeek
import com.apptolast.greenhousefronts.domain.model.IrrigationConfig
import com.apptolast.greenhousefronts.domain.model.SectorIrrigationConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IrrigationConfigUiState(
    val isLoading: Boolean = true,
    val config: IrrigationConfig? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val showConfirmDialog: Boolean = false,
)

private const val IRRIGATOR_PARAM_ID: Short = 18
private const val REGANDO_DEVICE_TYPE_ID: Short = 35
private const val EN_COLA_DEVICE_TYPE_ID: Short = 36

private const val SETTING_HORA_INICIO = "HORA INICIO"
private const val SETTING_MINUTO_INICIO = "MINUTO INICIO"
private const val SETTING_HORA_FIN = "HORA FIN"
private const val SETTING_MINUTO_FIN = "MINUTO FIN"
private const val SETTING_ESPERA_ENTRE_RIEGOS = "ESPERA ENTRE RIEGOS"
private const val SETTING_RIEGO_MANUAL = "RIEGO MANUAL"
private const val SETTING_TIEMPO_APERTURA = "TIEMPO APERTURA"
private const val SETTING_TIEMPO_ESPERA = "TIEMPO ESPERA"

private val DAY_NAME_MAP = mapOf(
    "LUNES" to DayOfWeek.MONDAY,
    "MARTES" to DayOfWeek.TUESDAY,
    "MIÉRCOLES" to DayOfWeek.WEDNESDAY,
    "JUEVES" to DayOfWeek.THURSDAY,
    "VIERNES" to DayOfWeek.FRIDAY,
    "SÁBADO" to DayOfWeek.SATURDAY,
    "DOMINGO" to DayOfWeek.SUNDAY,
)

/**
 * ViewModel for the irrigation configuration screen.
 * Loads data via WebSocket STOMP, saves via REST PUT per setting.
 */
class IrrigationConfigViewModel(
    private val webSocket: GreenhouseStatusWebSocket,
    private val commandApiService: CommandApiService,
) : ViewModel() {

    val uiState: StateFlow<IrrigationConfigUiState>
        field = MutableStateFlow(IrrigationConfigUiState())

    // Stores setting codes for save: "HORA INICIO" -> "SET-00036", sectorId -> {"TIEMPO APERTURA" -> "SET-00040"}
    private var globalSettingCodes: Map<String, String> = emptyMap()
    private var sectorSettingCodes: Map<Long, Map<String, String>> = emptyMap()

    private var wsJob: Job? = null

    /**
     * Tracks whether the editable config has been loaded.
     * Once true, subsequent WebSocket updates only refresh real-time status fields
     * (isIrrigating, isInQueue) without overwriting user edits in the form.
     */
    private var configLoaded = false

    fun loadConfig(greenhouseId: Long) {
        println("$TAG loadConfig(greenhouseId=$greenhouseId)")
        configLoaded = false
        startStatusUpdates(greenhouseId)
    }

    /**
     * Starts collecting the WebSocket statusFlow for continuous updates.
     * - First emission: populates the full config (schedule, days, sectors, real-time status)
     * - Subsequent emissions: only updates real-time status (isIrrigating, isInQueue)
     *   and refreshes setting IDs, without overwriting user form edits
     */
    private fun startStatusUpdates(greenhouseId: Long) {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            webSocket.statusFlow()
                .catch { e ->
                    println("$TAG WebSocket flow error: ${e::class.simpleName}: ${e.message}")
                    // Don't surface the raw exception — show a user-friendly message and
                    // let the WebSocket's own retryWhen reconnect in the background.
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se pudo conectar al servidor en tiempo real. Reintentando…",
                        )
                    }
                }
                .collect { status ->
                    val greenhouse = status.tenants
                        .flatMap { it.greenhouses }
                        .find { it.id == greenhouseId }

                    if (greenhouse == null) {
                        // While the first snapshot is in flight we may receive a frame for
                        // a different greenhouse (replay = 1 from the previous detail). Only
                        // surface an error once we've already loaded data and lost it.
                        if (configLoaded) {
                            uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Este invernadero ya no está disponible para tu cuenta.",
                                )
                            }
                        }
                        return@collect
                    }

                    val sectors = greenhouse.sectors.sortedBy { it.name }

                    // Always update setting codes (needed for save)
                    updateSettingCodes(sectors)

                    // Always compute real-time status
                    val realTimeStatus = computeRealTimeStatus(sectors)

                    if (!configLoaded) {
                        // First load: populate everything
                        val config = buildFullConfig(greenhouse.id, greenhouse.name, sectors, realTimeStatus)
                        println("$TAG Initial config: ${config.sectorConfigs.size} sectors, irrigating=${config.isIrrigating}")
                        configLoaded = true
                        uiState.update { it.copy(isLoading = false, config = config) }
                    } else {
                        // Subsequent updates: only refresh real-time status
                        val currentConfig = uiState.value.config ?: return@collect
                        uiState.update {
                            it.copy(
                                config = currentConfig.copy(
                                    isIrrigating = realTimeStatus.isIrrigating,
                                    irrigationStatus = realTimeStatus.irrigationStatus,
                                    isInQueue = realTimeStatus.isInQueue,
                                    queueStatus = realTimeStatus.queueStatus,
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun updateSettingCodes(sectors: List<WsSectorResponse>) {
        val globalSector = sectors.maxByOrNull { s ->
            s.settings.count { it.parameter?.id == IRRIGATOR_PARAM_ID }
        }
        val globalSettings = globalSector?.settings
            ?.filter { it.parameter?.id == IRRIGATOR_PARAM_ID }
            ?: emptyList()

        globalSettingCodes = globalSettings.associate {
            (it.actuatorState?.name ?: "") to it.code
        }
        sectorSettingCodes = sectors.associate { sector ->
            sector.id to sector.settings
                .filter { it.parameter?.id == IRRIGATOR_PARAM_ID }
                .associate { (it.actuatorState?.name ?: "") to it.code }
        }
    }

    private data class RealTimeStatus(
        val isIrrigating: Boolean,
        val irrigationStatus: String?,
        val isInQueue: Boolean,
        val queueStatus: String?,
    )

    private fun computeRealTimeStatus(sectors: List<WsSectorResponse>): RealTimeStatus {
        val isIrrigating = sectors.any { sector ->
            sector.devices.any {
                it.type?.id == REGANDO_DEVICE_TYPE_ID && it.currentValue.isTrueLike()
            }
        }
        val irrigatingSector = if (isIrrigating) {
            sectors.firstOrNull { sector ->
                sector.devices.any {
                    it.type?.id == REGANDO_DEVICE_TYPE_ID && it.currentValue.isTrueLike()
                }
            }
        } else null

        val isInQueue = sectors.any { sector ->
            sector.devices.any {
                it.type?.id == EN_COLA_DEVICE_TYPE_ID && it.currentValue.isTrueLike()
            }
        }
        val queueSector = if (isInQueue) {
            sectors.firstOrNull { sector ->
                sector.devices.any {
                    it.type?.id == EN_COLA_DEVICE_TYPE_ID && it.currentValue.isTrueLike()
                }
            }
        } else null

        return RealTimeStatus(
            isIrrigating = isIrrigating,
            irrigationStatus = irrigatingSector?.let { "${it.name} - Válvula abierta" },
            isInQueue = isInQueue,
            queueStatus = queueSector?.let { "${it.name} - En espera" },
        )
    }

    private fun buildFullConfig(
        greenhouseId: Long,
        greenhouseName: String,
        sectors: List<WsSectorResponse>,
        realTimeStatus: RealTimeStatus,
    ): IrrigationConfig {
        val globalSector = sectors.maxByOrNull { s ->
            s.settings.count { it.parameter?.id == IRRIGATOR_PARAM_ID }
        }
        val globalSettings = globalSector?.settings
            ?.filter { it.parameter?.id == IRRIGATOR_PARAM_ID }
            ?: emptyList()

        val startHour = globalSettings.intValue(SETTING_HORA_INICIO) ?: 11
        val startMinute = globalSettings.intValue(SETTING_MINUTO_INICIO) ?: 30
        val endHour = globalSettings.intValue(SETTING_HORA_FIN) ?: 18
        val endMinute = globalSettings.intValue(SETTING_MINUTO_FIN) ?: 30
        val waitBetween = globalSettings.intValue(SETTING_ESPERA_ENTRE_RIEGOS) ?: 90

        val activeDays = DAY_NAME_MAP.entries
            .filter { (dayName, _) -> globalSettings.boolValue(dayName) ?: false }
            .map { it.value }
            .toSet()

        val sectorConfigs = sectors.map { parseSectorConfig(it) }

        return IrrigationConfig(
            greenhouseId = greenhouseId,
            greenhouseName = greenhouseName,
            isIrrigating = realTimeStatus.isIrrigating,
            irrigationStatus = realTimeStatus.irrigationStatus,
            isInQueue = realTimeStatus.isInQueue,
            queueStatus = realTimeStatus.queueStatus,
            activeDays = activeDays.ifEmpty { DayOfWeek.entries.toSet() },
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            waitBetweenMinutes = waitBetween,
            sectorConfigs = sectorConfigs,
        )
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
        println("$TAG ViewModel cleared, WebSocket job cancelled")
    }

    private fun parseSectorConfig(sector: WsSectorResponse): SectorIrrigationConfig {
        val settings = sector.settings.filter { it.parameter?.id == IRRIGATOR_PARAM_ID }
        return SectorIrrigationConfig(
            sectorId = sector.id,
            sectorName = sector.name ?: sector.code,
            openingMinutes = settings.intValue(SETTING_TIEMPO_APERTURA) ?: 0,
            waitMinutes = settings.intValue(SETTING_TIEMPO_ESPERA) ?: 4,
            isActive = settings.any { it.isActive },
        )
    }

    // --- Save with confirmation dialog ---

    fun requestSave() {
        uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmSave() {
        uiState.update { it.copy(showConfirmDialog = false) }
        saveConfig()
    }

    private fun saveConfig() {
        val config = uiState.value.config ?: return
        if (uiState.value.isSaving) return

        viewModelScope.launch {
            uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

            try {
                var savedCount = 0

                // Send global settings as commands (schedule, days, wait)
                savedCount += sendGlobalCommand(SETTING_HORA_INICIO, config.startHour.toString())
                savedCount += sendGlobalCommand(SETTING_MINUTO_INICIO, config.startMinute.toString())
                savedCount += sendGlobalCommand(SETTING_HORA_FIN, config.endHour.toString())
                savedCount += sendGlobalCommand(SETTING_MINUTO_FIN, config.endMinute.toString())
                savedCount += sendGlobalCommand(
                    SETTING_ESPERA_ENTRE_RIEGOS,
                    config.waitBetweenMinutes.toString(),
                )

                // Send day settings as commands
                DAY_NAME_MAP.forEach { (dayName, day) ->
                    val isActive = day in config.activeDays
                    savedCount += sendGlobalCommand(dayName, isActive.toString())
                }

                // Send per-sector settings as commands
                config.sectorConfigs.forEach { sector ->
                    val codes = sectorSettingCodes[sector.sectorId] ?: return@forEach
                    codes[SETTING_TIEMPO_APERTURA]?.let { code ->
                        commandApiService.sendCommand(code, sector.openingMinutes.toString())
                        savedCount++
                    }
                    codes[SETTING_TIEMPO_ESPERA]?.let { code ->
                        commandApiService.sendCommand(code, sector.waitMinutes.toString())
                        savedCount++
                    }
                }

                println("$TAG Sent $savedCount commands successfully")
                uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                println("$TAG SAVE ERROR: ${e::class.simpleName}: ${e.message}")
                uiState.update {
                    it.copy(isSaving = false, error = "Error al guardar: ${e.message}")
                }
            }
        }
    }

    private suspend fun sendGlobalCommand(actuatorName: String, value: String): Int {
        val code = globalSettingCodes[actuatorName] ?: return 0
        commandApiService.sendCommand(code, value)
        return 1
    }

    // --- Helpers ---

    private fun List<WsSettingResponse>.intValue(actuatorName: String): Int? =
        find { it.actuatorState?.name == actuatorName }?.currentValue?.toIntOrNull()

    private fun List<WsSettingResponse>.boolValue(actuatorName: String): Boolean? {
        val v = find { it.actuatorState?.name == actuatorName }?.currentValue ?: return null
        return v.isTrueLike()
    }

    // --- Form updates ---

    fun toggleDay(day: DayOfWeek) {
        val c = uiState.value.config ?: return
        val d = c.activeDays.toMutableSet()
        if (day in d) d.remove(day) else d.add(day)
        uiState.update { it.copy(config = c.copy(activeDays = d)) }
    }

    fun updateStartHour(v: Int) {
        val c = uiState.value.config ?: return
        uiState.update { it.copy(config = c.copy(startHour = v.coerceIn(0, 23))) }
    }

    fun updateStartMinute(v: Int) {
        val c = uiState.value.config ?: return
        uiState.update { it.copy(config = c.copy(startMinute = v.coerceIn(0, 59))) }
    }

    fun updateEndHour(v: Int) {
        val c = uiState.value.config ?: return
        uiState.update { it.copy(config = c.copy(endHour = v.coerceIn(0, 23))) }
    }

    fun updateEndMinute(v: Int) {
        val c = uiState.value.config ?: return
        uiState.update { it.copy(config = c.copy(endMinute = v.coerceIn(0, 59))) }
    }

    fun updateWaitBetween(v: Int) {
        val c = uiState.value.config ?: return
        uiState.update { it.copy(config = c.copy(waitBetweenMinutes = v.coerceAtLeast(0))) }
    }

    fun updateSectorOpening(i: Int, min: Int) {
        val c = uiState.value.config ?: return
        val u = c.sectorConfigs.toMutableList()
        if (i in u.indices) {
            u[i] = u[i].copy(openingMinutes = min.coerceAtLeast(0))
            uiState.update { it.copy(config = c.copy(sectorConfigs = u)) }
        }
    }

    fun updateSectorWait(i: Int, min: Int) {
        val c = uiState.value.config ?: return
        val u = c.sectorConfigs.toMutableList()
        if (i in u.indices) {
            u[i] = u[i].copy(waitMinutes = min.coerceAtLeast(0))
            uiState.update { it.copy(config = c.copy(sectorConfigs = u)) }
        }
    }

    fun toggleSectorActive(i: Int) {
        val c = uiState.value.config ?: return
        val u = c.sectorConfigs.toMutableList()
        if (i in u.indices) {
            u[i] = u[i].copy(isActive = !u[i].isActive)
            uiState.update { it.copy(config = c.copy(sectorConfigs = u)) }
        }
    }

    fun clearSaveSuccess() {
        uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "[IRRIGATION-VM]"
    }
}
