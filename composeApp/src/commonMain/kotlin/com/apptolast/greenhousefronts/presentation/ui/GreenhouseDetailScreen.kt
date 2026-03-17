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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.Device
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.domain.model.SectorWithDevices
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GreenhouseDetailScreen(
    greenhouseId: Long,
    viewModel: GreenhouseDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToIrrigationConfig: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(greenhouseId) {
        viewModel.loadGreenhouse(greenhouseId)
    }

    GreenhouseDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleActive = viewModel::toggleActive,
        onNavigateToIrrigationConfig = onNavigateToIrrigationConfig,
        onSelectSector = viewModel::selectSector,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GreenhouseDetailContent(
    uiState: GreenhouseDetailUiState,
    onNavigateBack: () -> Unit,
    onToggleActive: () -> Unit,
    onNavigateToIrrigationConfig: (Long) -> Unit,
    onSelectSector: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.greenhouse?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    uiState.greenhouse?.let { greenhouse ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = if (greenhouse.isActive) "Activo" else "Inactivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (greenhouse.isActive) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = greenhouse.isActive,
                                onCheckedChange = { onToggleActive() },
                                enabled = !uiState.isTogglingActive,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            )
                        }
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
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            uiState.greenhouse?.let { greenhouse ->
                GreenhouseDetailBody(
                    greenhouse = greenhouse,
                    sectors = uiState.sectors,
                    selectedSectorIndex = uiState.selectedSectorIndex,
                    onNavigateToIrrigationConfig = onNavigateToIrrigationConfig,
                    onSelectSector = onSelectSector,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GreenhouseDetailBody(
    greenhouse: Greenhouse,
    sectors: List<SectorWithDevices>,
    selectedSectorIndex: Int,
    onNavigateToIrrigationConfig: (Long) -> Unit,
    onSelectSector: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Alert banner
        if (greenhouse.alertCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "${greenhouse.alertCount} ${if (greenhouse.alertCount == 1) "alerta activa" else "alertas activas"} en este invernadero",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            greenhouse.areaM2?.let { area ->
                val formatted = if (area == area.toLong().toDouble()) {
                    "${area.toLong()} m²"
                } else {
                    "$area m²"
                }
                StatCard(value = formatted, label = "Área", modifier = Modifier.weight(1f))
            }

            if (greenhouse.alertCount > 0) {
                StatCard(
                    value = greenhouse.alertCount.toString(),
                    label = "Alertas",
                    modifier = Modifier.weight(1f),
                    highlighted = true,
                )
            }
        }

        // Irrigation config card
        IrrigationConfigCard(
            onClick = { onNavigateToIrrigationConfig(greenhouse.id) },
        )

        // Selectable sector chips
        if (sectors.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sectors.forEachIndexed { index, sector ->
                    SectorChip(
                        name = sector.name,
                        isSelected = index == selectedSectorIndex,
                        onClick = { onSelectSector(index) },
                    )
                }
            }
        }

        // Devices section
        val selectedSector = sectors.getOrNull(selectedSectorIndex)
        val deviceCount = selectedSector?.devices?.size ?: 0

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title = "Dispositivos")
            if (deviceCount > 0) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = deviceCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (selectedSector != null && selectedSector.devices.isNotEmpty()) {
            // Grid layout: 2 columns
            val chunked = selectedSector.devices.chunked(2)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { device ->
                        DeviceCard(device = device, modifier = Modifier.weight(1f))
                    }
                    // Fill empty space if odd number
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Text(
                text = "Sin dispositivos en este sector",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- Components ---

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectorChip(
    name: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DeviceCard(
    device: Device,
    modifier: Modifier = Modifier,
) {
    val statusColor = when {
        device.currentValue == null -> Color(0xFF666666)
        !device.isActive -> Color(0xFFEF5350)
        else -> Color(0xFF4CAF50)
    }

    val displayValue = formatDeviceValue(device.currentValue, device.unitSymbol)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            // Top row: icon area + status dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Device type icon placeholder
                Text(
                    text = deviceTypeEmoji(device.typeName),
                    fontSize = 22.sp,
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device name
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Value + unit inline
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                device.unitSymbol?.let { unit ->
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

private fun formatDeviceValue(value: String?, unitSymbol: String?): String {
    if (value == null) return "--"
    // Format boolean values
    if (value.lowercase() == "true") return "ON"
    if (value.lowercase() == "false") return "OFF"
    // Format large numbers (e.g., 45000 lux → 45K)
    val numValue = value.toDoubleOrNull()
    if (numValue != null && numValue >= 10000) {
        return "${(numValue / 1000).toInt()}K"
    }
    // Format decimals: remove unnecessary trailing zeros
    if (numValue != null && numValue == numValue.toLong().toDouble()) {
        return numValue.toLong().toString()
    }
    return value
}

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

@Composable
private fun IrrigationConfigCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Configuración de Riego",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Programación semanal y sectores",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Previews ---

@Preview
@Composable
private fun PreviewGreenhouseDetail() {
    GreenhouseTheme(darkTheme = true) {
        GreenhouseDetailContent(
            uiState = GreenhouseDetailUiState(
                isLoading = false,
                greenhouse = Greenhouse(
                    id = 1L, code = "GRH-00001", name = "Invernadero Norte",
                    isActive = true, areaM2 = 2500.0, sectorCount = 3, alertCount = 3,
                    sectorNames = listOf("Sector A", "Sector B", "Sector C"),
                ),
                sectors = listOf(
                    SectorWithDevices(
                        1L, "SEC-00001", "Sector A",
                        devices = listOf(
                            Device(
                                1L,
                                "DEV-001",
                                "Temperatura",
                                true,
                                "SENSOR",
                                "TEMPERATURE",
                                1,
                                "°C",
                                "23.5",
                                null,
                                -50.0,
                                100.0,
                                null
                            ),
                            Device(
                                2L,
                                "DEV-002",
                                "Humedad",
                                true,
                                "SENSOR",
                                "HUMIDITY",
                                2,
                                "%",
                                "65",
                                null,
                                0.0,
                                100.0,
                                null
                            ),
                            Device(
                                3L,
                                "DEV-003",
                                "CO2",
                                true,
                                "SENSOR",
                                "CO2 LEVEL",
                                5,
                                "ppm",
                                "820",
                                null,
                                0.0,
                                5000.0,
                                null
                            ),
                            Device(
                                4L,
                                "DEV-004",
                                "Extractor",
                                true,
                                "ACTUATOR",
                                "EXTRACTOR",
                                24,
                                "%",
                                "75",
                                null,
                                null,
                                null,
                                "CONTINUOUS"
                            ),
                        ),
                    ),
                    SectorWithDevices(2L, "SEC-00002", "Sector B", devices = emptyList()),
                    SectorWithDevices(3L, "SEC-00003", "Sector C", devices = emptyList()),
                ),
                selectedSectorIndex = 0,
            ),
            onNavigateBack = {},
            onToggleActive = {},
            onNavigateToIrrigationConfig = {},
            onSelectSector = {},
        )
    }
}
