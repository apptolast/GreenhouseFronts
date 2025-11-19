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
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
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
    if (statistics.chartData.isNotEmpty()) {
        val modelProducer = remember { CartesianChartModelProducer() }
        val lineColor = MaterialTheme.colorScheme.primary

        // Update model with chart data
        LaunchedEffect(statistics.chartData) {
            modelProducer.runTransaction {
                lineSeries {
                    series(statistics.chartData.map { it.value })
                }
            }
        }

        CartesianChartHost(
            rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.Line(
                            // Neon green line with gradient fill
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
                        )
                    )
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom()
            ),
            modelProducer,
            modifier
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
