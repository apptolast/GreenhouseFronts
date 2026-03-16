package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import com.apptolast.greenhousefronts.data.remote.websocket.WsSectorResponse
import com.apptolast.greenhousefronts.data.remote.websocket.WsSettingResponse
import com.apptolast.greenhousefronts.domain.model.DayOfWeek
import com.apptolast.greenhousefronts.domain.model.IrrigationConfig
import com.apptolast.greenhousefronts.domain.model.SectorIrrigationConfig
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

// parameterId=18 identifies all irrigation-related settings
private const val IRRIGATOR_PARAM_ID: Short = 18

// Device type id=35 "REGANDO" indicates real-time irrigation status
private const val REGANDO_DEVICE_TYPE_ID: Short = 35

// actuatorState.name values used by the backend for individual settings
private const val SETTING_HORA_INICIO = "HORA INICIO"
private const val SETTING_MINUTO_INICIO = "MINUTO INICIO"
private const val SETTING_HORA_FIN = "HORA FIN"
private const val SETTING_MINUTO_FIN = "MINUTO FIN"
private const val SETTING_ESPERA_ENTRE_RIEGOS = "ESPERA ENTRE RIEGOS"
private const val SETTING_RIEGO_MANUAL = "RIEGO MANUAL"
private const val SETTING_TIEMPO_APERTURA = "TIEMPO APERTURA"
private const val SETTING_TIEMPO_ESPERA = "TIEMPO ESPERA"

// Map day names from actuatorState.name to DayOfWeek
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
 * Loads data from the WebSocket STOMP status endpoint.
 *
 * The backend stores irrigation config as individual settings per field:
 * - Each setting has parameterId=18 (IRRIGATOR)
 * - actuatorState.name identifies the field (e.g., "HORA INICIO", "LUNES", "TIEMPO APERTURA")
 * - currentValue holds the actual value from TimescaleDB
 * - Global settings (schedule, days) are on Sector 00 (the first sector)
 * - Per-sector settings (TIEMPO APERTURA, TIEMPO ESPERA) are on each sector
 */
class IrrigationConfigViewModel(
    private val webSocket: GreenhouseStatusWebSocket,
) : ViewModel() {

    val uiState: StateFlow<IrrigationConfigUiState>
        field = MutableStateFlow(IrrigationConfigUiState())

    fun loadConfig(greenhouseId: Long) {
        println("$TAG loadConfig(greenhouseId=$greenhouseId)")
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val status = webSocket.requestStatus()

                val greenhouse = status.tenants
                    .flatMap { it.greenhouses }
                    .find { it.id == greenhouseId }

                if (greenhouse == null) {
                    println("$TAG Greenhouse id=$greenhouseId NOT FOUND")
                    uiState.update {
                        it.copy(isLoading = false, error = "Invernadero no encontrado")
                    }
                    return@launch
                }

                val sectors = greenhouse.sectors.sortedBy { it.name }
                println("$TAG Found ${greenhouse.name}: ${sectors.size} sectors")

                // Global settings come from the first sector that has the full config (13 settings)
                val globalSector = sectors.maxByOrNull { sector ->
                    sector.settings.count { it.parameter?.id == IRRIGATOR_PARAM_ID }
                }
                val globalSettings = globalSector?.settings
                    ?.filter { it.parameter?.id == IRRIGATOR_PARAM_ID }
                    ?: emptyList()

                println("$TAG Global settings sector: '${globalSector?.name}' with ${globalSettings.size} irrigation settings")

                // Parse global schedule from individual settings
                val startHour = globalSettings.intValue(SETTING_HORA_INICIO) ?: 11
                val startMinute = globalSettings.intValue(SETTING_MINUTO_INICIO) ?: 30
                val endHour = globalSettings.intValue(SETTING_HORA_FIN) ?: 18
                val endMinute = globalSettings.intValue(SETTING_MINUTO_FIN) ?: 30
                val waitBetween = globalSettings.intValue(SETTING_ESPERA_ENTRE_RIEGOS) ?: 90

                println("$TAG Schedule: $startHour:$startMinute - $endHour:$endMinute, wait=$waitBetween")

                // Parse active days from individual boolean settings
                val activeDays = DAY_NAME_MAP.entries
                    .filter { (dayName, _) ->
                        globalSettings.boolValue(dayName) ?: false
                    }
                    .map { it.value }
                    .toSet()

                println("$TAG Active days: ${activeDays.map { it.shortLabel }}")

                // Check real-time irrigation status via REGANDO device type
                val isIrrigating = sectors.any { sector ->
                    sector.devices.any { device ->
                        device.type?.id == REGANDO_DEVICE_TYPE_ID &&
                                device.currentValue?.lowercase() == "true"
                    }
                }

                val irrigatingStatus = if (isIrrigating) {
                    val activeSector = sectors.firstOrNull { sector ->
                        sector.devices.any { device ->
                            device.type?.id == REGANDO_DEVICE_TYPE_ID &&
                                    device.currentValue?.lowercase() == "true"
                        }
                    }
                    "${activeSector?.name ?: "Sector"} - Válvula abierta"
                } else {
                    null
                }

                // Build per-sector configs
                val sectorConfigs = sectors.map { sector ->
                    parseSectorConfig(sector)
                }

                val config = IrrigationConfig(
                    greenhouseId = greenhouse.id,
                    greenhouseName = greenhouse.name,
                    isIrrigating = isIrrigating,
                    irrigationStatus = irrigatingStatus,
                    activeDays = activeDays.ifEmpty { DayOfWeek.entries.toSet() },
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    waitBetweenMinutes = waitBetween,
                    sectorConfigs = sectorConfigs,
                )
                println("$TAG Config built: ${config.sectorConfigs.size} sectors, irrigating=${config.isIrrigating}")
                config.sectorConfigs.forEach {
                    println("$TAG   ${it.sectorName}: apertura=${it.openingMinutes}, espera=${it.waitMinutes}, active=${it.isActive}")
                }

                uiState.update { it.copy(isLoading = false, config = config) }
            } catch (e: Exception) {
                println("$TAG ERROR: ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "${e::class.simpleName}: ${e.message}",
                    )
                }
            }
        }
    }

    /**
     * Parses per-sector irrigation settings.
     * Each sector has TIEMPO APERTURA and TIEMPO ESPERA settings,
     * and a REGANDO device for real-time status.
     */
    private fun parseSectorConfig(sector: WsSectorResponse): SectorIrrigationConfig {
        val irrigationSettings = sector.settings.filter {
            it.parameter?.id == IRRIGATOR_PARAM_ID
        }

        val openingMinutes = irrigationSettings.intValue(SETTING_TIEMPO_APERTURA) ?: 0
        val waitMinutes = irrigationSettings.intValue(SETTING_TIEMPO_ESPERA) ?: 4

        // Sector is active if it has irrigation settings that are active
        val isActive = irrigationSettings.any { it.isActive }

        return SectorIrrigationConfig(
            sectorId = sector.id,
            sectorName = sector.name ?: sector.code,
            openingMinutes = openingMinutes,
            waitMinutes = waitMinutes,
            isActive = isActive,
        )
    }

    // --- Helper extensions to read values from settings by actuatorState.name ---

    /**
     * Finds a setting by actuatorState.name and returns its currentValue as Int.
     */
    private fun List<WsSettingResponse>.intValue(actuatorName: String): Int? {
        return find { it.actuatorState?.name == actuatorName }
            ?.currentValue
            ?.toIntOrNull()
    }

    /**
     * Finds a setting by actuatorState.name and returns its currentValue as Boolean.
     */
    private fun List<WsSettingResponse>.boolValue(actuatorName: String): Boolean? {
        val value = find { it.actuatorState?.name == actuatorName }?.currentValue ?: return null
        return value.lowercase() == "true" || value == "1"
    }

    // --- Form update methods ---

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

            // TODO: Send configuration to backend Settings API
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

    companion object {
        private const val TAG = "[IRRIGATION-VM]"
    }
}
