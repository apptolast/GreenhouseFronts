package com.apptolast.greenhousefronts.presentation.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific line chart implementation.
 *
 * Uses Vico for native platforms (Android, iOS, Desktop)
 * Uses AAY-chart for web platforms (JS, WasmJS)
 *
 * @param values Y-axis values (e.g., temperature readings)
 * @param labels X-axis labels (e.g., formatted timestamps)
 * @param unitLabel Unit label for the Y-axis (e.g., "°C")
 * @param modifier Modifier for the chart composable
 */
@Composable
expect fun PlatformLineChart(
    values: List<Double>,
    labels: List<String>,
    unitLabel: String,
    modifier: Modifier,
)
