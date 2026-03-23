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

@Composable
actual fun PlatformLineChart(
    values: List<Double>,
    labels: List<String>,
    unitLabel: String,
    modifier: Modifier,
    scrollEnabled: Boolean,
) {
    if (values.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Sin datos disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = values.indices.map { it.toDouble() },
                    y = values,
                )
            }
        }
    }

    // Vico 2.4.3 does NOT allow empty strings from CartesianValueFormatter.
    // Always return a non-empty label. Use the provided label or the index as fallback.
    val xAxisFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            val label = labels.getOrNull(index)
            if (!label.isNullOrBlank()) label else index.toString()
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            Fill(
                                Brush.verticalGradient(
                                    listOf(
                                        lineColor.copy(alpha = 0.4f),
                                        lineColor.copy(alpha = 0.0f),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            startAxis = VerticalAxis.rememberStart(
                guideline = null,
                label = TextComponent(textStyle = TextStyle(color = labelColor)),
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                guideline = null,
                valueFormatter = xAxisFormatter,
                label = TextComponent(textStyle = TextStyle(color = labelColor)),
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = scrollEnabled),
    )
}
