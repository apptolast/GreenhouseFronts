package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod
import com.apptolast.greenhousefronts.presentation.ui.chart.PlatformLineChart
import com.apptolast.greenhousefronts.presentation.viewmodel.SensorDetailViewModel
import com.apptolast.greenhousefronts.util.formatDecimals
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.action_retry
import greenhousefronts.composeapp.generated.resources.empty_state
import greenhousefronts.composeapp.generated.resources.error_unknown
import greenhousefronts.composeapp.generated.resources.period_last_24h
import greenhousefronts.composeapp.generated.resources.period_last_30d
import greenhousefronts.composeapp.generated.resources.period_last_7d
import greenhousefronts.composeapp.generated.resources.sensor_detail_back
import greenhousefronts.composeapp.generated.resources.sensor_detail_no_chart_data
import greenhousefronts.composeapp.generated.resources.sensor_detail_title
import greenhousefronts.composeapp.generated.resources.sensor_type_with_unit
import greenhousefronts.composeapp.generated.resources.stat_average
import greenhousefronts.composeapp.generated.resources.stat_max
import greenhousefronts.composeapp.generated.resources.stat_min
import org.jetbrains.compose.resources.stringResource

/**
 * Sensor detail screen showing historical data and statistics.
 *
 * @param greenhouseId UUID of the greenhouse
 * @param sensorType Type of sensor (TEMPERATURE or HUMIDITY)
 * @param viewModel The ViewModel managing state and business logic
 * @param onNavigateBack Callback for back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailScreen(
    greenhouseId: String,
    sensorType: SensorType,
    viewModel: SensorDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize ViewModel with parameters
    LaunchedEffect(greenhouseId, sensorType) {
        viewModel.initialize(greenhouseId, sensorType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.sensor_detail_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.sensor_detail_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                uiState.error != null -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(Res.string.error_unknown),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                uiState.statistics != null -> {
                    // Success state - show data
                    SensorDetailContent(
                        statistics = uiState.statistics!!,
                        sensorType = sensorType,
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodChange = { viewModel.changePeriod(it) }
                    )
                }

                else -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content displayed when data is loaded successfully
 */
@Composable
private fun SensorDetailContent(
    statistics: SensorStatistics,
    sensorType: SensorType,
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Sensor title
        Text(
            text = sensorType.displayName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Period filter chips
        PeriodFilterRow(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Current value section
        Column {
            Text(
                text = stringResource(
                    Res.string.sensor_type_with_unit,
                    sensorType.displayName,
                    statistics.unit
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${statistics.currentValue.formatDecimals(1)}${statistics.unit}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trend indicator
            TrendIndicator(
                period = getPeriodDisplayName(selectedPeriod),
                trendPercent = statistics.trendPercent,
                trendDirection = statistics.trendDirection
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Chart
        HistoricalChart(
            statistics = statistics,
            sensorType = sensorType
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Statistics cards
        StatisticsCardsGrid(
            avgValue = statistics.avgValue,
            maxValue = statistics.maxValue,
            minValue = statistics.minValue,
            unit = statistics.unit
        )
    }
}

/**
 * Row of period filter chips
 */
@Composable
private fun PeriodFilterRow(
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimePeriod.entries.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodChange(period) },
                label = { Text(getPeriodDisplayName(period)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Trend indicator showing percentage change and direction
 */
@Composable
private fun TrendIndicator(
    period: String,
    trendPercent: Double,
    trendDirection: String
) {
    val arrow = when (trendDirection) {
        "INCREASING" -> "↑"
        "DECREASING" -> "↓"
        else -> "→"
    }

    val color = when (trendDirection) {
        "INCREASING" -> Color(0xFF00E676) // Green
        "DECREASING" -> Color(0xFFFF6B6B) // Red
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = period,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$arrow ${if (trendPercent >= 0) "+" else ""}${trendPercent.formatDecimals(1)}%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Historical data line chart using platform-specific implementation.
 * - Vico for Android, iOS, Desktop
 * - AAY-chart for Web (JS, WasmJS)
 */
@Composable
private fun HistoricalChart(
    statistics: SensorStatistics,
    sensorType: SensorType
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            PlatformLineChart(
                statistics = statistics,
                sensorType = sensorType,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Grid of statistics cards (Average, Max, Min)
 */
@Composable
private fun StatisticsCardsGrid(
    avgValue: Double,
    maxValue: Double,
    minValue: Double,
    unit: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: Average and Max
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = stringResource(Res.string.stat_average),
                value = "${avgValue.formatDecimals(1)}$unit",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(Res.string.stat_max),
                value = "${maxValue.formatDecimals(1)}$unit",
                modifier = Modifier.weight(1f)
            )
        }

        // Second row: Min (full width)
        StatCard(
            label = stringResource(Res.string.stat_min),
            value = "${minValue.formatDecimals(1)}$unit",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Individual statistics card
 */
@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Helper function to get localized display name for a time period
 */
@Composable
private fun getPeriodDisplayName(period: TimePeriod): String {
    return when (period) {
        TimePeriod.LAST_24H -> stringResource(Res.string.period_last_24h)
        TimePeriod.LAST_7D -> stringResource(Res.string.period_last_7d)
        TimePeriod.LAST_30D -> stringResource(Res.string.period_last_30d)
    }
}
