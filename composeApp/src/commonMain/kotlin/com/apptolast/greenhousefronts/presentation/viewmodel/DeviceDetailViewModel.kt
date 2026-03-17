package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class ChartPeriod(val label: String, val hoursAgo: Long) {
    DAY("Día", 24),
    WEEK("Semana", 168),
    MONTH("Mes", 720),
    YEAR("Año", 8760),
    ALL("Todo", 87600),
}

data class ChartPoint(val timestamp: String, val value: Double)

data class DeviceStats(
    val current: Double?,
    val average: Double?,
    val min: Double?,
    val max: Double?,
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

    private fun loadChartData(deviceCode: String, period: ChartPeriod) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoadingChart = true) }

            try {
                val readings = sensorApiService.getReadingsByCode(deviceCode, period.hoursAgo)

                val points = readings
                    .mapNotNull { reading ->
                        val value = reading.value.toDoubleOrNull() ?: return@mapNotNull null
                        ChartPoint(reading.time, value)
                    }
                    .sortedBy { it.timestamp }

                if (points.isNotEmpty()) {
                    applyChartData(points, period)
                } else {
                    // No data from API — use mock data for visualization
                    println("[DEVICE-VM] No readings from API, using mock data")
                    applyMockChartData(period)
                }
            } catch (e: Exception) {
                println("[DEVICE-VM] Chart data error: ${e.message}, using mock data")
                applyMockChartData(period)
            }
        }
    }

    private fun applyChartData(points: List<ChartPoint>, period: ChartPeriod) {
        val values = points.map { it.value }
        val labels = generateLabels(points, period)
        val stats = DeviceStats(
            current = uiState.value.device?.currentValue?.toDoubleOrNull(),
            average = values.average(),
            min = values.min(),
            max = values.max(),
        )
        uiState.update {
            it.copy(
                isLoadingChart = false,
                chartPoints = points,
                chartValues = values,
                chartLabels = labels,
                stats = stats
            )
        }
    }

    private fun applyMockChartData(period: ChartPeriod) {
        val baseValue = uiState.value.device?.currentValue?.toDoubleOrNull() ?: 22.0
        val count = when (period) {
            ChartPeriod.DAY -> 24
            ChartPeriod.WEEK -> 7 * 24
            ChartPeriod.MONTH -> 30
            ChartPeriod.YEAR -> 12
            ChartPeriod.ALL -> 24
        }
        val random = kotlin.random.Random(42)
        val values = (0 until count).map { baseValue + random.nextDouble(-3.0, 3.0) }
        val labels = (0 until count).map { i ->
            when (period) {
                ChartPeriod.DAY -> "${i}:00"
                ChartPeriod.WEEK -> "D${i / 24 + 1}"
                ChartPeriod.MONTH -> "${i + 1}"
                ChartPeriod.YEAR -> "M${i + 1}"
                ChartPeriod.ALL -> "${i}:00"
            }
        }
        val stats = DeviceStats(
            current = uiState.value.device?.currentValue?.toDoubleOrNull(),
            average = values.average(),
            min = values.min(),
            max = values.max(),
        )
        uiState.update {
            it.copy(isLoadingChart = false, chartValues = values, chartLabels = labels, stats = stats)
        }
    }

    private fun generateLabels(points: List<ChartPoint>, period: ChartPeriod): List<String> {
        if (points.isEmpty()) return emptyList()

        val step = (points.size / 6).coerceAtLeast(1)
        return points.mapIndexed { index, point ->
            if (index % step == 0) {
                // Extract time from ISO timestamp
                val time = point.timestamp.substringAfter("T").substringBefore(".")
                when (period) {
                    ChartPeriod.DAY -> time.take(5) // HH:mm
                    ChartPeriod.WEEK, ChartPeriod.MONTH -> point.timestamp.substringBefore("T").takeLast(5) // MM-dd
                    ChartPeriod.YEAR, ChartPeriod.ALL -> point.timestamp.substringBefore("T").take(7) // YYYY-MM
                }
            } else {
                ""
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
