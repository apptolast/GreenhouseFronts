package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.model.sensor.HistoricalDataResponse
import com.apptolast.greenhousefronts.data.remote.api.SensorApiService
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import com.apptolast.greenhousefronts.data.remote.websocket.WsSettingResponse
import com.apptolast.greenhousefronts.domain.model.Device
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChartPeriod(val label: String, val hoursAgo: Long, val apiPeriod: String) {
    DAY("Día", 24, "24h"),
    WEEK("Semana", 168, "7d"),
    MONTH("Mes", 720, "720h"),
    YEAR("Año", 8760, "8760h"),
    ALL("Todo", 87600, "all"),
}

data class ChartPoint(val timestamp: String, val value: Double)

data class DeviceStats(
    val current: Double?,
    val average: Double?,
    val min: Double?,
    val max: Double?,
)

data class BooleanTransition(
    val timestamp: String,
    val displayTime: String,
    val newState: Boolean,
)

data class BooleanDeviceStats(
    val currentState: Boolean?,
    val transitionCount: Int,
    val onPercentage: Double,
    val offPercentage: Double,
)

data class DeviceDetailUiState(
    val isLoading: Boolean = true,
    val device: Device? = null,
    val sectorName: String = "",
    val greenhouseName: String = "",
    val isLive: Boolean = false,
    val chartPoints: List<ChartPoint> = emptyList(),
    val chartValues: List<Double> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    val stats: DeviceStats = DeviceStats(null, null, null, null),
    val selectedPeriod: ChartPeriod = ChartPeriod.DAY,
    val settings: List<WsSettingResponse> = emptyList(),
    val isLoadingChart: Boolean = false,
    val isChartExpanded: Boolean = false,
    val isBooleanDevice: Boolean = false,
    val transitions: List<BooleanTransition> = emptyList(),
    val booleanStats: BooleanDeviceStats? = null,
    val error: String? = null,
)

/**
 * ViewModel for device detail screen.
 * Uses WebSocket for real-time value updates and REST for historical chart data.
 */
class DeviceDetailViewModel(
    private val webSocket: GreenhouseStatusWebSocket,
    private val sensorApiService: SensorApiService,
) : ViewModel() {

    val uiState: StateFlow<DeviceDetailUiState>
        field = MutableStateFlow(DeviceDetailUiState())

    private var wsJob: Job? = null

    fun loadDevice(deviceCode: String, greenhouseId: Long) {
        // Start real-time updates via WebSocket
        startLiveUpdates(deviceCode, greenhouseId)
        // Load historical chart data
        loadChartData(deviceCode, uiState.value.selectedPeriod)
    }

    fun selectPeriod(period: ChartPeriod) {
        uiState.update { it.copy(selectedPeriod = period) }
        val code = uiState.value.device?.code ?: return
        loadChartData(code, period)
    }

    fun toggleChartExpanded() {
        uiState.update { it.copy(isChartExpanded = !it.isChartExpanded) }
    }

    private fun startLiveUpdates(deviceCode: String, greenhouseId: Long) {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            webSocket.statusFlow()
                .catch { e ->
                    println("[DEVICE-VM] WebSocket error: ${e.message}")
                    uiState.update { it.copy(isLive = false) }
                }
                .collect { status ->
                    val greenhouse = status.tenants
                        .flatMap { it.greenhouses }
                        .find { it.id == greenhouseId }

                    if (greenhouse != null) {
                        for (sector in greenhouse.sectors) {
                            val wsDevice = sector.devices.find { it.code == deviceCode }
                            if (wsDevice != null) {
                                val device = Device(
                                    id = wsDevice.id,
                                    code = wsDevice.code,
                                    name = wsDevice.name ?: wsDevice.code,
                                    clientName = wsDevice.clientName,
                                    isActive = wsDevice.isActive,
                                    categoryName = wsDevice.category?.name ?: "",
                                    typeName = wsDevice.type?.name ?: "",
                                    typeId = wsDevice.type?.id ?: 0,
                                    unitSymbol = wsDevice.unit?.symbol?.takeIf { it != "-" },
                                    currentValue = wsDevice.currentValue,
                                    lastUpdated = wsDevice.lastUpdated,
                                    minExpectedValue = wsDevice.type?.minExpectedValue,
                                    maxExpectedValue = wsDevice.type?.maxExpectedValue,
                                    controlType = wsDevice.type?.controlType,
                                    dataType = wsDevice.type?.dataType,
                                )

                                // Get settings from the same sector for "consigna configurada"
                                val sectorSettings = sector.settings

                                uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        device = device,
                                        sectorName = sector.name ?: sector.code,
                                        greenhouseName = greenhouse.name,
                                        isLive = true,
                                        settings = sectorSettings,
                                        stats = it.stats.copy(
                                            current = device.currentValue?.toDoubleOrNull(),
                                        ),
                                    )
                                }
                                break
                            }
                        }
                    }
                }
        }
    }

    /**
     * Loads historical data from the adaptive statistics endpoint.
     * The backend decides resolution (raw/hourly/daily/monthly) and detects boolean devices.
     * Single endpoint handles both numeric charts and boolean transitions.
     */
    private fun loadChartData(deviceCode: String, period: ChartPeriod) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoadingChart = true) }

            try {
                val response = sensorApiService.getHistoricalData(deviceCode, period.apiPeriod)

                if (response.isBooleanDevice) {
                    applyBooleanResponse(response)
                } else if (response.chartData.isNotEmpty()) {
                    applyNumericResponse(response, period)
                } else {
                    clearChartData()
                }
            } catch (e: Exception) {
                println("[DEVICE-VM] Chart data error: ${e.message}")
                clearChartData()
            }
        }
    }

    private fun applyNumericResponse(response: HistoricalDataResponse, period: ChartPeriod) {
        val points = response.chartData.map { ChartPoint(it.timestamp, it.value) }
        val values = points.map { it.value }
        val labels = points.map { formatLabel(it.timestamp, period) }

        val stats = DeviceStats(
            current = response.currentValue
                ?: uiState.value.device?.currentValue?.toDoubleOrNull(),
            average = response.avgValue,
            min = response.minValue,
            max = response.maxValue,
        )

        uiState.update {
            it.copy(
                isLoadingChart = false,
                chartPoints = points,
                chartValues = values,
                chartLabels = labels,
                stats = stats,
                isBooleanDevice = false,
                transitions = emptyList(),
                booleanStats = null,
            )
        }
    }

    private fun applyBooleanResponse(response: HistoricalDataResponse) {
        val transitions = response.transitions?.map { dto ->
            BooleanTransition(
                timestamp = dto.timestamp,
                displayTime = formatBooleanTimestamp(dto.timestamp),
                newState = dto.newState,
            )
        } ?: emptyList()

        val booleanStats = response.booleanStats?.let { dto ->
            BooleanDeviceStats(
                currentState = parseBooleanValue(uiState.value.device?.currentValue),
                transitionCount = dto.transitionCount,
                onPercentage = dto.onPercentage,
                offPercentage = dto.offPercentage,
            )
        }

        uiState.update {
            it.copy(
                isLoadingChart = false,
                isBooleanDevice = true,
                transitions = transitions,
                booleanStats = booleanStats,
                chartPoints = emptyList(),
                chartValues = emptyList(),
                chartLabels = emptyList(),
            )
        }
    }

    private fun clearChartData() {
        uiState.update {
            it.copy(
                isLoadingChart = false,
                chartPoints = emptyList(),
                chartValues = emptyList(),
                chartLabels = emptyList(),
                stats = DeviceStats(
                    current = uiState.value.device?.currentValue?.toDoubleOrNull(),
                    average = null,
                    min = null,
                    max = null,
                ),
                isBooleanDevice = false,
                transitions = emptyList(),
                booleanStats = null,
            )
        }
    }

    /** Parses boolean values from "true"/"false", "1"/"0", and "1.0"/"0.0" formats. */
    private fun parseBooleanValue(value: String?): Boolean? {
        return when (value?.lowercase()) {
            "true", "1", "1.0" -> true
            "false", "0", "0.0" -> false
            else -> null
        }
    }

    /** Formats ISO timestamp to "dd/MM/yyyy - HH:mm:ss". */
    private fun formatBooleanTimestamp(isoTimestamp: String): String {
        val datePart = isoTimestamp.substringBefore("T")
        val timePart = isoTimestamp.substringAfter("T")
            .substringBefore(".")
            .substringBefore("Z")
        val parts = datePart.split("-")
        return if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]} - $timePart"
        } else {
            "$datePart - $timePart"
        }
    }

    /**
     * Formats a single ISO timestamp into a chart label based on the selected period.
     * Never returns an empty string (Vico 2.4.3 requirement).
     */
    private fun formatLabel(isoTimestamp: String, period: ChartPeriod): String {
        // timestamp format: "2026-03-17T13:30:00.123Z"
        val datePart = isoTimestamp.substringBefore("T") // "2026-03-17"
        val timePart = isoTimestamp.substringAfter("T").substringBefore(".").substringBefore("Z") // "13:30:00"
        val parts = datePart.split("-") // ["2026", "03", "17"]

        return when (period) {
            ChartPeriod.DAY -> timePart.take(5) // "13:30"

            ChartPeriod.WEEK -> {
                // Hour with day in parentheses: "10h (17)"
                val hour = timePart.take(2).trimStart('0').ifEmpty { "0" }
                val day = if (parts.size == 3) parts[2].trimStart('0').ifEmpty { "0" } else "?"
                "${hour}h ($day)"
            }

            ChartPeriod.MONTH -> {
                // Day and month: "17/03"
                if (parts.size == 3) "${parts[2]}/${parts[1]}" else datePart.takeLast(5)
            }

            ChartPeriod.YEAR -> {
                // Day and month: "17/03"
                if (parts.size == 3) "${parts[2]}/${parts[1]}" else datePart.takeLast(5)
            }

            ChartPeriod.ALL -> {
                // Month and year: "03/26"
                if (parts.size >= 2) "${parts[1]}/${parts[0].takeLast(2)}" else datePart.take(7)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }
}
