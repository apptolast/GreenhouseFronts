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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.apptolast.greenhousefronts.data.model.SensorStatistics
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.data.model.TimePeriod
import com.apptolast.greenhousefronts.presentation.ui.chart.PlatformLineChart
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.SensorDetailUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.SensorDetailViewModel
import com.apptolast.greenhousefronts.util.formatDecimals
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.action_retry
import greenhousefronts.composeapp.generated.resources.empty_state
import greenhousefronts.composeapp.generated.resources.error_load_statistics
import greenhousefronts.composeapp.generated.resources.error_unknown
import greenhousefronts.composeapp.generated.resources.period_last_24h
import greenhousefronts.composeapp.generated.resources.period_last_30d
import greenhousefronts.composeapp.generated.resources.period_last_7d
import greenhousefronts.composeapp.generated.resources.sensor_detail_back
import greenhousefronts.composeapp.generated.resources.sensor_detail_title
import greenhousefronts.composeapp.generated.resources.sensor_type_with_unit
import greenhousefronts.composeapp.generated.resources.stat_average
import greenhousefronts.composeapp.generated.resources.stat_max
import greenhousefronts.composeapp.generated.resources.stat_min
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Sensor detail screen (Stateful).
 * It observes the ViewModel's state and handles events.
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
        SensorDetailContent(
            uiState = uiState,
            sensorType = sensorType,
            onPeriodChange = { viewModel.changePeriod(it) },
            onRetry = { viewModel.retry() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }
}

/**
 * Content for the sensor detail screen (Stateless).
 * Displays the appropriate UI based on the uiState.
 */
@Composable
private fun SensorDetailContent(
    uiState: SensorDetailUiState,
    sensorType: SensorType,
    modifier: Modifier = Modifier,
    onPeriodChange: (TimePeriod) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.error?.let {
                            stringResource(
                                Res.string.error_load_statistics,
                                it
                            )
                        } ?: stringResource(Res.string.error_unknown),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }

            uiState.statistics != null -> {
                SensorDetailSuccessContent(
                    statistics = uiState.statistics,
                    sensorType = sensorType,
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodChange = onPeriodChange
                )
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

@Preview
@Composable
private fun SensorDetailContentLoadingPreview() {
    GreenhouseTheme {
        SensorDetailContent(
            uiState = SensorDetailUiState(isLoading = true),
            sensorType = SensorType.TEMPERATURE,
        )
    }
}

@Preview
@Composable
private fun SensorDetailContentErrorPreview() {
    GreenhouseTheme {
        SensorDetailContent(
            uiState = SensorDetailUiState(isLoading = false, error = "Network error"),
            sensorType = SensorType.TEMPERATURE,
        )
    }
}

@Preview
@Composable
private fun SensorDetailContentSuccessPreview() {
    SensorDetailContent(
        uiState = SensorDetailUiState(
            isLoading = false,
            statistics = SensorStatistics.dummyData(),
            selectedPeriod = TimePeriod.LAST_7D
        ),
        sensorType = SensorType.TEMPERATURE,
    )
}

@Preview
@Composable
private fun SensorDetailContentEmptyPreview() {
    SensorDetailContent(
        uiState = SensorDetailUiState(isLoading = false, statistics = null, error = null),
        sensorType = SensorType.TEMPERATURE,
    )
}

/**
 * Content displayed when data is loaded successfully.
 */
@Composable
private fun SensorDetailSuccessContent(
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
        Text(
            sensorType.displayName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        PeriodFilterRow(selectedPeriod = selectedPeriod, onPeriodChange = onPeriodChange)
        Spacer(modifier = Modifier.height(24.dp))
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
            TrendIndicator(
                period = getPeriodDisplayName(selectedPeriod),
                trendPercent = statistics.trendPercent,
                trendDirection = statistics.trendDirection
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        HistoricalChart(
            statistics = statistics,
            sensorType = sensorType,
            selectedPeriod = selectedPeriod
        )
        Spacer(modifier = Modifier.height(32.dp))
        val stats = listOf(
            StatItem(
                label = stringResource(Res.string.stat_average),
                value = "${statistics.avgValue.formatDecimals(1)}${statistics.unit}"
            ),
            StatItem(
                label = stringResource(Res.string.stat_max),
                value = "${statistics.maxValue.formatDecimals(1)}${statistics.unit}"
            ),
            StatItem(
                label = stringResource(Res.string.stat_min),
                value = "${statistics.minValue.formatDecimals(1)}${statistics.unit}"
            )
        )
        StatisticsCardsGrid(stats = stats)
    }
}

@Preview
@Composable
private fun SensorDetailSuccessContentPreview() {
    GreenhouseTheme {
        SensorDetailSuccessContent(
            statistics = SensorStatistics.dummyData(),
            sensorType = SensorType.TEMPERATURE,
            selectedPeriod = TimePeriod.LAST_7D,
            onPeriodChange = {}
        )
    }
}

@Composable
private fun PeriodFilterRow(selectedPeriod: TimePeriod, onPeriodChange: (TimePeriod) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun TrendIndicator(period: String, trendPercent: Double, trendDirection: String) {
    val arrow = when (trendDirection) {
        "INCREASING" -> "↑"
        "DECREASING" -> "↓"
        else -> "→"
    }
    val color = when (trendDirection) {
        "INCREASING" -> Color(0xFF00E676)
        "DECREASING" -> Color(0xFFFF6B6B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            period,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$arrow ${if (trendPercent >= 0) "+" else ""}${trendPercent.formatDecimals(1)}%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun HistoricalChart(
    statistics: SensorStatistics,
    sensorType: SensorType,
    selectedPeriod: TimePeriod
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            PlatformLineChart(
                statistics = statistics,
                sensorType = sensorType,
                selectedPeriod = selectedPeriod,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private data class StatItem(val label: String, val value: String)

@Composable
private fun StatisticsCardsGrid(stats: List<StatItem>) {
    // We must provide a fixed height to a lazy component inside a scrollable column to avoid performance issues and crashes.
    // Calculate row count: ceiling division of stats.size by 2.
    val rowCount = (stats.size + 1) / 2
    // Estimate height: (card height * rows) + (spacing * (rows - 1)).
    // A single card is roughly 100dp tall. We add 12dp for vertical spacing between rows.
    val gridHeight = (rowCount * 100 + (rowCount - 1) * 12).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // The parent is already scrolling, so we disable scrolling for the grid itself.
        userScrollEnabled = false
    ) {
        items(stats) { stat ->
            StatCard(
                label = stat.label,
                value = stat.value
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun getPeriodDisplayName(period: TimePeriod): String {
    return when (period) {
        TimePeriod.LAST_24H -> stringResource(Res.string.period_last_24h)
        TimePeriod.LAST_7D -> stringResource(Res.string.period_last_7d)
        TimePeriod.LAST_30D -> stringResource(Res.string.period_last_30d)
    }
}
