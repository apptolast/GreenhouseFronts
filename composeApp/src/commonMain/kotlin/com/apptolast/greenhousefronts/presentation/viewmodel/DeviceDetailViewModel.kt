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
    val isChartExpanded: Boolean = false,
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
        val expanded = !uiState.value.isChartExpanded
        uiState.update { it.copy(isChartExpanded = expanded) }
        // Re-apply chart data with different bucketing
        val points = uiState.value.chartPoints
        if (points.isNotEmpty()) {
            applyChartData(points, uiState.value.selectedPeriod)
        }
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

    /**
     * Applies real API data to the chart.
     * - Condensed mode (default): reduces to ~24 points, no scrolling needed.
     * - Expanded mode: keeps more data points for scrollable detail view.
     */
    private fun applyChartData(points: List<ChartPoint>, period: ChartPeriod) {
        val isExpanded = uiState.value.isChartExpanded
        val maxPoints = if (isExpanded) points.size else 24
        val reduced = if (points.size > maxPoints) {
            val bucketSize = points.size / maxPoints
            points.chunked(bucketSize).map { bucket ->
                ChartPoint(
                    timestamp = bucket[bucket.size / 2].timestamp,
                    value = bucket.map { it.value }.average(),
                )
            }
        } else {
            points
        }

        val values = reduced.map { it.value }
        val labelStep = (reduced.size / 6).coerceAtLeast(1)
        val labels = reduced.mapIndexed { i, point ->
            if (i % labelStep == 0 || i == reduced.lastIndex) {
                formatLabel(point.timestamp, period)
            } else {
                ""
            }
        }
        val allValues = points.map { it.value }

        val stats = DeviceStats(
            current = uiState.value.device?.currentValue?.toDoubleOrNull(),
            average = allValues.average(),
            min = allValues.min(),
            max = allValues.max(),
        )
        uiState.update {
            it.copy(
                isLoadingChart = false,
                chartPoints = points,
                chartValues = values,
                chartLabels = labels,
                stats = stats,
            )
        }
    }

    private fun applyMockChartData(period: ChartPeriod) {
        val baseValue = uiState.value.device?.currentValue?.toDoubleOrNull() ?: 22.0
        val random = kotlin.random.Random(42)
        val count = 24

        val values = (0 until count).map { baseValue + random.nextDouble(-2.0, 2.0) }
        val labels = when (period) {
            ChartPeriod.DAY -> (0 until count).map { "${it}:00" }
            ChartPeriod.WEEK -> (0 until count).map { "D${it / 3 + 1}" }
            ChartPeriod.MONTH -> (0 until count).map { "${it + 1}" }
            ChartPeriod.YEAR -> (0 until count).map { "M${it / 2 + 1}" }
            ChartPeriod.ALL -> (0 until count).map { "${it}:00" }
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

    /**
     * Formats a single ISO timestamp into a chart label based on the selected period.
     * Never returns an empty string (Vico 2.4.3 requirement).
     */
    private fun formatLabel(isoTimestamp: String, period: ChartPeriod): String {
        // timestamp format: "2026-03-17T13:30:00.123Z"
        val datePart = isoTimestamp.substringBefore("T") // "2026-03-17"
        val timePart = isoTimestamp.substringAfter("T").substringBefore(".").substringBefore("Z") // "13:30:00"

        return when (period) {
            ChartPeriod.DAY -> timePart.take(5) // "13:30"
            ChartPeriod.WEEK -> {
                // Just the day number to avoid repetitive "17/03" labels
                val parts = datePart.split("-")
                if (parts.size == 3) parts[2].trimStart('0').ifEmpty { "0" } else datePart.takeLast(2)
            }

            ChartPeriod.MONTH -> {
                // Day number only (1-31)
                val parts = datePart.split("-")
                if (parts.size == 3) parts[2].trimStart('0').ifEmpty { "0" } else datePart.takeLast(2)
            }

            ChartPeriod.YEAR -> {
                val parts = datePart.split("-")
                if (parts.size >= 2) "${parts[1]}/${parts[0].takeLast(2)}" else datePart.take(7)
            }

            ChartPeriod.ALL -> {
                val parts = datePart.split("-")
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
