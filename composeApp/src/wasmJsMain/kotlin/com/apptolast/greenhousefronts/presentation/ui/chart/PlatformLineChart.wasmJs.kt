package com.apptolast.greenhousefronts.presentation.ui.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.aay.compose.lineChart.LineChart
import com.aay.compose.lineChart.model.LineParameters
import com.aay.compose.lineChart.model.LineType
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod
import com.apptolast.greenhousefronts.util.generateMockData
import com.apptolast.greenhousefronts.util.selectXAxisLabels
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.sensor_detail_no_chart_data
import org.jetbrains.compose.resources.stringResource

/**
 * WebAssembly (Wasm) platform implementation using AAY-chart.
 * Provides web-compatible charting with smooth curved lines.
 */
@Composable
actual fun PlatformLineChart(
    statistics: SensorStatistics,
    sensorType: SensorType,
    selectedPeriod: TimePeriod,
    modifier: Modifier
) {
    // Generate or use real data
    val chartData = remember(selectedPeriod, statistics.chartData) {
        // Use mock data for testing - Replace with real data when available
        if (statistics.chartData.size < 5) {
            generateMockData(selectedPeriod, baseValue = statistics.currentValue)
        } else {
            statistics.chartData
        }
    }

    if (chartData.isNotEmpty()) {
        // Convert chartData to LineChart format
        val chartValues = chartData.map { it.value }

        // Get timestamps as Long values
        val timestamps = chartData.map { it.timestamp.toLongOrNull() ?: 0L }

        // Use shared utility to select and format X-axis labels
        val labelPairs = selectXAxisLabels(timestamps, selectedPeriod, maxLabels = 6)

        // Create full list of labels (empty strings for non-selected indices)
        val xAxisLabels = MutableList(chartData.size) { "" }
        labelPairs.forEach { (index, label) ->
            if (index < xAxisLabels.size) {
                xAxisLabels[index] = label
            }
        }

        LineChart(
            modifier = modifier,
            linesParameters = listOf(
                LineParameters(
                    label = sensorType.displayName,
                    data = chartValues,
                    lineColor = MaterialTheme.colorScheme.primary,
                    lineType = LineType.CURVED_LINE,
                    lineShadow = true
                )
            ),
            gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            xAxisData = xAxisLabels,
            animateChart = true,
            showGridWithSpacer = true,
            yAxisStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            xAxisStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            showXAxis = true,
            showYAxis = true
        )
    } else {
        // No chart data
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.sensor_detail_no_chart_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
