package com.apptolast.greenhousefronts.presentation.ui.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.aay.compose.lineChart.LineChart
import com.aay.compose.lineChart.model.LineParameters
import com.aay.compose.lineChart.model.LineType
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.sensor_detail_no_chart_data
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime

/**
 * JavaScript platform implementation using AAY-chart.
 * Provides web-compatible charting with smooth curved lines.
 */
@Composable
actual fun PlatformLineChart(
    statistics: SensorStatistics,
    sensorType: SensorType,
    modifier: Modifier
) {
    if (statistics.chartData.isNotEmpty()) {
        // Convert chartData to LineChart format
        val chartValues = statistics.chartData.map { it.value }

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
            xAxisData = formatXAxisLabels(statistics.chartData.map { it.timestamp }),
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

/**
 * Formats timestamps for X-axis labels
 * Shows time for 24h period, dates for longer periods
 */
@OptIn(ExperimentalTime::class)
private fun formatXAxisLabels(timestamps: List<String>): List<String> {
    if (timestamps.isEmpty()) return emptyList()

    return try {
        // Take subset of timestamps for display (show ~5-6 labels max)
        val step = maxOf(1, timestamps.size / 5)
        val selectedTimestamps = timestamps.filterIndexed { index, _ ->
            index % step == 0 || index == timestamps.lastIndex
        }

        selectedTimestamps.map { timestamp ->
            try {
                val instant = kotlin.time.Instant.parse(timestamp)
                val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

                // If showing 24h data, show time (HH:mm)
                // If showing longer period, show date (MM/DD)
                if (timestamps.size < 48) { // Less than 48 points = likely hourly data (24h or less)
                    "${dateTime.hour.toString().padStart(2, '0')}:${
                        dateTime.minute.toString().padStart(2, '0')
                    }"
                } else { // Daily or longer data
                    "${dateTime.month.ordinal + 1}/${dateTime.day}"
                }
            } catch (e: Exception) {
                ""
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}
