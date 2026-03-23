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
                "Sin datos disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    LineChart(
        modifier = modifier,
        linesParameters = listOf(
            LineParameters(
                label = unitLabel,
                data = values,
                lineColor = MaterialTheme.colorScheme.primary,
                lineType = LineType.CURVED_LINE,
                lineShadow = true,
            ),
        ),
        gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        xAxisData = labels,
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
        showYAxis = true,
    )
}
