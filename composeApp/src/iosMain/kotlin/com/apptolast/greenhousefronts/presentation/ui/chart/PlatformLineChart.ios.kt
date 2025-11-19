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
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod
import com.apptolast.greenhousefronts.util.formatXAxisLabel
import com.apptolast.greenhousefronts.util.generateMockData
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
import org.jetbrains.compose.resources.stringResource

/**
 * iOS platform implementation using Vico.
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
