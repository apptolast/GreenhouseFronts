package com.apptolast.greenhousefronts.presentation.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod

/**
 * Platform-specific line chart implementation.
 *
 * Uses Vico for native platforms (Android, iOS, Desktop)
 * Uses AAY-chart for web platforms (JS, WasmJS)
 *
 * @param statistics Sensor statistics containing chart data
 * @param sensorType Type of sensor for labeling
 * @param modifier Modifier for the chart composable
 */
@Composable
expect fun PlatformLineChart(
    statistics: SensorStatistics,
    sensorType: SensorType,
    selectedPeriod: TimePeriod,
    modifier: Modifier
)
