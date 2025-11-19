package com.apptolast.greenhousefronts.presentation.ui.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import com.apptolast.greenhousefronts.data.model.ChartDataPoint
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.component.TextComponent
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.sensor_detail_no_chart_data
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Generates mock data for testing with realistic timestamps
 */
@OptIn(ExperimentalTime::class)
private fun generateMockData(period: TimePeriod, baseValue: Double = 15.0): List<ChartDataPoint> {
    val now = Clock.System.now()
    val dataPoints = mutableListOf<ChartDataPoint>()

    when (period) {
        TimePeriod.LAST_24H -> {
            // Generate 48 points (one every 30 minutes for 24h)
            repeat(48) { index ->
                val timestamp = now.minus(24.hours - (index * 30).minutes)
                val value = baseValue + Random.nextDouble(-10.0, 10.0)
                dataPoints.add(
                    ChartDataPoint(
                        timestamp = timestamp.toEpochMilliseconds().toString(),
                        value = value
                    )
                )
            }
        }

        TimePeriod.LAST_7D -> {
            // Generate 28 points (one every 6 hours for 7 days)
            repeat(28) { index ->
                val timestamp = now.minus(7.days - (index * 6).hours)
                val value = baseValue + Random.nextDouble(-10.0, 10.0)
                dataPoints.add(
                    ChartDataPoint(
                        timestamp = timestamp.toEpochMilliseconds().toString(),
                        value = value
                    )
                )
            }
        }

        TimePeriod.LAST_30D -> {
            // Generate 30 points (one per day for 30 days)
            repeat(30) { index ->
                val timestamp = now.minus(30.days - (index).days)
                val value = baseValue + Random.nextDouble(-10.0, 10.0)
                dataPoints.add(
                    ChartDataPoint(
                        timestamp = timestamp.toEpochMilliseconds().toString(),
                        value = value
                    )
                )
            }
        }
    }

    return dataPoints
}

/**
 * Formats timestamp for X-axis labels based on time period
 */
@OptIn(ExperimentalTime::class)
private fun formatXAxisLabel(timestampMillis: Long, period: TimePeriod): String {
    val instant = Instant.fromEpochMilliseconds(timestampMillis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    return when (period) {
        TimePeriod.LAST_24H -> {
            // Show hours: "00:00", "06:00", "12:00", etc.
            "${dateTime.hour.toString().padStart(2, '0')}:${
                dateTime.minute.toString().padStart(2, '0')
            }"
        }

        TimePeriod.LAST_7D -> {
            // Show day abbreviation and day: "Lun 15", "Mar 16", etc.
            val dayOfWeek = when (dateTime.dayOfWeek) {
                DayOfWeek.MONDAY -> "Lun"
                DayOfWeek.TUESDAY -> "Mar"
                DayOfWeek.WEDNESDAY -> "Mié"
                DayOfWeek.THURSDAY -> "Jue"
                DayOfWeek.FRIDAY -> "Vie"
                DayOfWeek.SATURDAY -> "Sáb"
                DayOfWeek.SUNDAY -> "Dom"
                else -> ""
            }
            "$dayOfWeek ${dateTime.dayOfMonth}"
        }

        TimePeriod.LAST_30D -> {
            // Show day of month: "1", "5", "10", "15", etc.
            "${dateTime.dayOfMonth}/${dateTime.monthNumber}"
        }
    }
}

/**
 * Android platform implementation using Vico.
 * Provides high-performance native charting with Material 3 integration.
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
        val modelProducer = remember { CartesianChartModelProducer() }

        // Theme color for chart line
        val lineColor = MaterialTheme.colorScheme.primary


        // Update model with chart data
        LaunchedEffect(chartData) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = chartData.map { it.timestamp.toLongOrNull() ?: 0L },
                        y = chartData.map { it.value }
                    )
                }
            }
        }

        // Custom value formatter for X-axis
        val xAxisValueFormatter = remember(selectedPeriod) {
            CartesianValueFormatter { value, chartValues, _ ->
                formatXAxisLabel(chartValues.toLong(), selectedPeriod)
            }
        }

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.Line(
                            fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),

                            // Gradient area fill from bright green to transparent
                            areaFill = LineCartesianLayer.AreaFill.single(
                                Fill(
                                    Brush.verticalGradient(
                                        listOf(
                                            lineColor.copy(alpha = 0.4f),  // Bright green with 40% opacity at top
                                            lineColor.copy(alpha = 0.0f)   // Fully transparent at bottom
                                        )
                                    )
                                )
                            )
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(
                    guideline = null,
                    title = sensorType.displayName,
                    titleComponent = TextComponent(
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ),
                    label = TextComponent(
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ),
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    guideline = null,
                    valueFormatter = xAxisValueFormatter,
                    title = "Time",
                    titleComponent = TextComponent(
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ),
                    label = TextComponent(
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ),
                ),
            ),
            modelProducer = modelProducer,
            modifier = modifier,
            scrollState = rememberVicoScrollState(scrollEnabled = true)
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

// Format example
// https://www.youtube.com/watch?v=MY290aW8hMQ