package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.Device
import com.apptolast.greenhousefronts.presentation.ui.chart.PlatformLineChart
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.BooleanDeviceStats
import com.apptolast.greenhousefronts.presentation.viewmodel.BooleanTransition
import com.apptolast.greenhousefronts.presentation.viewmodel.ChartPeriod
import com.apptolast.greenhousefronts.presentation.viewmodel.DeviceDetailUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.DeviceDetailViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.DeviceStats
import com.apptolast.greenhousefronts.util.isFalseLike
import com.apptolast.greenhousefronts.util.isTrueLike
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DeviceDetailScreen(
    deviceCode: String,
    greenhouseId: Long,
    viewModel: DeviceDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(deviceCode, greenhouseId) {
        viewModel.loadDevice(deviceCode, greenhouseId)
    }

    DeviceDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSelectPeriod = viewModel::selectPeriod,
        onToggleChartExpanded = viewModel::toggleChartExpanded,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetailContent(
    uiState: DeviceDetailUiState,
    onNavigateBack: () -> Unit,
    onSelectPeriod: (ChartPeriod) -> Unit,
    onToggleChartExpanded: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.device?.clientName ?: uiState.device?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (uiState.sectorName.isNotBlank()) {
                            Text(
                                text = "${uiState.sectorName} - ${uiState.greenhouseName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LoadingBar(isLoading = uiState.isLoading || uiState.isLoadingChart)

            uiState.device?.let { device ->
                DeviceDetailBody(
                    uiState = uiState,
                    onSelectPeriod = onSelectPeriod,
                    onToggleChartExpanded = onToggleChartExpanded,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceDetailBody(
    uiState: DeviceDetailUiState,
    onSelectPeriod: (ChartPeriod) -> Unit,
    onToggleChartExpanded: () -> Unit,
) {
    val device = uiState.device ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Current value hero
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = deviceTypeEmoji(device.typeName),
                fontSize = 40.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = formatDeviceValue(device.currentValue, device.unitSymbol),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                device.unitSymbol?.let { unit ->
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            Text(
                text = "Lectura actual",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Period selector chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChartPeriod.entries.forEach { period ->
                PeriodChip(
                    label = period.label,
                    isSelected = period == uiState.selectedPeriod,
                    onClick = { onSelectPeriod(period) },
                )
            }
        }

        // Chart section header with expand/collapse toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Histórico",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = onToggleChartExpanded) {
                Icon(
                    imageVector = if (uiState.isChartExpanded) {
                        Icons.Filled.UnfoldLess
                    } else {
                        Icons.Filled.UnfoldMore
                    },
                    contentDescription = if (uiState.isBooleanDevice) {
                        if (uiState.isChartExpanded) "Mostrar menos cambios" else "Mostrar todos los cambios"
                    } else {
                        if (uiState.isChartExpanded) "Condensar grafica" else "Expandir grafica"
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            if (uiState.isBooleanDevice) {
                BooleanTransitionList(
                    transitions = uiState.transitions,
                    isExpanded = uiState.isChartExpanded,
                )
            } else if (uiState.chartValues.isNotEmpty()) {
                PlatformLineChart(
                    values = uiState.chartValues,
                    labels = uiState.chartLabels,
                    unitLabel = device.unitSymbol ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp),
                    scrollEnabled = uiState.isChartExpanded,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sin datos historicos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Statistics section
        Text(
            text = "Estadisticas del periodo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (uiState.isBooleanDevice && uiState.booleanStats != null) {
            BooleanStatsGrid(stats = uiState.booleanStats)
        } else {
            StatsGrid(stats = uiState.stats, unitSymbol = device.unitSymbol)
        }

        // Device info
        Text(
            text = "Información del dispositivo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow("Nombre interno", device.name)
                InfoRow("Código", device.code)
                InfoRow("Categoría", device.categoryName)
                InfoRow("Tipo", device.typeName)
                device.unitSymbol?.let { InfoRow("Unidad", it) }
                InfoRow("Estado", if (device.isActive) "Activo" else "Inactivo")
            }
        }

        // Settings (consigna configurada) if available
        val relevantSettings = uiState.settings.filter {
            it.parameter?.id == device.typeId && it.currentValue != null
        }
        if (relevantSettings.isNotEmpty()) {
            Text(
                text = "Consigna configurada",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    relevantSettings.forEach { setting ->
                        val label = setting.actuatorState?.name ?: setting.description ?: setting.code
                        val value = setting.currentValue ?: "--"
                        InfoRow(label, value)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PeriodChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun StatsGrid(stats: DeviceStats, unitSymbol: String?) {
    val unit = unitSymbol ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(label = "Actual", value = stats.current?.formatStat(unit) ?: "--", modifier = Modifier.weight(1f))
            StatBox(label = "Promedio", value = stats.average?.formatStat(unit) ?: "--", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(
                label = "Mínimo",
                value = stats.min?.formatStat(unit) ?: "--",
                modifier = Modifier.weight(1f),
                icon = "↓"
            )
            StatBox(
                label = "Máximo",
                value = stats.max?.formatStat(unit) ?: "--",
                modifier = Modifier.weight(1f),
                icon = "↑"
            )
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, icon: String? = null) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BooleanTransitionList(
    transitions: List<BooleanTransition>,
    isExpanded: Boolean,
) {
    val maxCollapsed = 10
    val displayedTransitions = if (isExpanded) transitions else transitions.takeLast(maxCollapsed)

    if (transitions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Sin cambios de estado en el periodo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (!isExpanded && transitions.size > maxCollapsed) {
                Text(
                    text = "Mostrando ultimos $maxCollapsed de ${transitions.size} cambios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            displayedTransitions.forEachIndexed { index, transition ->
                TransitionRow(transition)
                if (index < displayedTransitions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitionRow(transition: BooleanTransition) {
    val stateColor = if (transition.newState) {
        Color(0xFF4CAF50)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(stateColor),
        )
        Text(
            text = transition.displayTime,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (transition.newState) "ON" else "OFF",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = stateColor,
        )
    }
}

@Composable
private fun BooleanStatsGrid(stats: BooleanDeviceStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(
                label = "Estado actual",
                value = when (stats.currentState) {
                    true -> "ON"
                    false -> "OFF"
                    null -> "--"
                },
                modifier = Modifier.weight(1f),
            )
            StatBox(
                label = "Cambios",
                value = stats.transitionCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(
                label = "Tiempo ON",
                value = "${stats.onPercentage.formatPercent()}%",
                modifier = Modifier.weight(1f),
                icon = "▲",
            )
            StatBox(
                label = "Tiempo OFF",
                value = "${stats.offPercentage.formatPercent()}%",
                modifier = Modifier.weight(1f),
                icon = "▼",
            )
        }
    }
}

private fun Double.formatPercent(): String {
    val rounded = (this * 10).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

// Reuse from GreenhouseDetailScreen
private fun deviceTypeEmoji(typeName: String): String {
    return when (typeName.uppercase()) {
        "TEMPERATURE", "TEMPERATURA DE DIA" -> "🌡️"
        "HUMIDITY", "SOIL MOISTURE" -> "💧"
        "CO2 LEVEL" -> "☁️"
        "LIGHT INTENSITY", "ILLUMINANCE" -> "☀️"
        "ATMOSPHERIC PRESSURE" -> "🌀"
        "WIND SPEED", "WIND GUST" -> "💨"
        "WIND DIRECTION" -> "🧭"
        "RAINFALL" -> "🌧️"
        "SOLAR RADIATION" -> "☀️"
        "PH" -> "🧪"
        "EC" -> "⚡"
        "UV INDEX" -> "🔆"
        "DEWPOINT" -> "🌡️"
        "VENTILATOR", "FAN", "EXTRACTOR" -> "🌀"
        "HEATER" -> "🔥"
        "COOLER" -> "❄️"
        "IRRIGATOR" -> "🚿"
        "LIGHTING" -> "💡"
        "CURTAIN", "WINDOW" -> "🪟"
        "VALVE" -> "🔧"
        "PUMP" -> "⛽"
        "MISTING" -> "🌊"
        "DEHUMIDIFIER" -> "🌬️"
        "CO2 INJECTOR" -> "🫧"
        "REGANDO" -> "🚿"
        "EN COLA" -> "⏳"
        "LIFE BEAT" -> "💓"
        else -> "📡"
    }
}

private fun Double.formatStat(unit: String): String {
    val rounded = (this * 10).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        "${rounded.toLong()}$unit"
    } else {
        "$rounded$unit"
    }
}

private fun formatDeviceValue(value: String?, unitSymbol: String?): String {
    if (value == null) return "--"
    // Accept legacy "1"/"0" defensively (pre-Phase-6 format).
    if (value.isTrueLike()) return "ON"
    if (value.isFalseLike()) return "OFF"
    val numValue = value.toDoubleOrNull()
    if (numValue != null && numValue >= 10000) return "${(numValue / 1000).toInt()}K" // FIXME: Check this hardcoded unitSymbol
    if (numValue != null && numValue == numValue.toLong().toDouble()) return numValue.toLong().toString()
    return value
}

@Preview
@Composable
private fun PreviewDeviceDetail() {
    GreenhouseTheme(darkTheme = true) {
        DeviceDetailContent(
            uiState = DeviceDetailUiState(
                isLoading = false,
                device = Device(
                    id = 1L,
                    code = "DEV-00031",
                    name = "WS90_TEMP_INTERIOR",
                    clientName = "Sensor de Temperatura",
                    isActive = true,
                    categoryName = "SENSOR",
                    typeName = "TEMPERATURE",
                    typeId = 1,
                    unitSymbol = "°C",
                    currentValue = "24.5",
                    lastUpdated = "2026-03-17T13:30:00Z",
                    minExpectedValue = -50.0,
                    maxExpectedValue = 100.0,
                    controlType = null,
                ),
                sectorName = "Sector A",
                greenhouseName = "Invernadero Norte",
                isLive = true,
                chartValues = listOf(22.1, 22.5, 23.0, 23.8, 24.1, 24.5, 24.2, 23.9, 24.0, 24.5),
                chartLabels = listOf("00:00", "", "", "06:00", "", "", "12:00", "", "", "18:00"),
                stats = DeviceStats(
                    current = 24.5,
                    average = 23.8,
                    min = 19.2,
                    max = 27.1,
                ),
                selectedPeriod = ChartPeriod.DAY,
            ),
            onNavigateBack = {},
            onSelectPeriod = {},
        )
    }
}

@Preview
@Composable
private fun PreviewDeviceDetailLoading() {
    GreenhouseTheme(darkTheme = true) {
        DeviceDetailContent(
            uiState = DeviceDetailUiState(isLoading = true),
            onNavigateBack = {},
            onSelectPeriod = {},
        )
    }
}

@Preview
@Composable
private fun PreviewDeviceDetailBoolean() {
    GreenhouseTheme(darkTheme = true) {
        DeviceDetailContent(
            uiState = DeviceDetailUiState(
                isLoading = false,
                device = Device(
                    id = 5L,
                    code = "DEV-00042",
                    name = "IRRIGATOR_01",
                    clientName = "Riego Sector A",
                    isActive = true,
                    categoryName = "ACTUATOR",
                    typeName = "REGANDO",
                    typeId = 10,
                    unitSymbol = null,
                    currentValue = "false",
                    lastUpdated = "2026-03-17T13:30:00Z",
                    minExpectedValue = null,
                    maxExpectedValue = null,
                    controlType = "ON_OFF",
                    dataType = "BOOLEAN",
                ),
                sectorName = "Sector 00",
                greenhouseName = "Invernadero 03",
                isLive = true,
                isBooleanDevice = true,
                transitions = listOf(
                    BooleanTransition("2026-03-17T08:00:00Z", "17/03/2026 - 08:00:00", true),
                    BooleanTransition("2026-03-17T09:15:32Z", "17/03/2026 - 09:15:32", false),
                    BooleanTransition("2026-03-17T10:30:00Z", "17/03/2026 - 10:30:00", true),
                    BooleanTransition("2026-03-17T12:45:18Z", "17/03/2026 - 12:45:18", false),
                    BooleanTransition("2026-03-17T13:30:00Z", "17/03/2026 - 13:30:00", true),
                ),
                booleanStats = BooleanDeviceStats(
                    currentState = false,
                    transitionCount = 4,
                    onPercentage = 62.5,
                    offPercentage = 37.5,
                ),
                selectedPeriod = ChartPeriod.DAY,
            ),
            onNavigateBack = {},
            onSelectPeriod = {},
        )
    }
}
