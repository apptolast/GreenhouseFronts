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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.Device
import com.apptolast.greenhousefronts.domain.model.Setpoint
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
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
    onNavigateToDeviceDetail: (String) -> Unit = {},
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
        onDeviceClick = onNavigateToDeviceDetail,
        onSetpointValueChange = viewModel::updateSetpointValue,
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
    onDeviceClick: (String) -> Unit = {},
    onSetpointValueChange: (setpointId: Long, newValue: String) -> Unit = { _, _ -> },
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
            LoadingBar(isLoading = uiState.isLoading)

            uiState.greenhouse?.let { greenhouse ->
                GreenhouseDetailBody(
                    greenhouse = greenhouse,
                    sectors = uiState.sectors,
                    selectedSectorIndex = uiState.selectedSectorIndex,
                    savingSetpointIds = uiState.savingSetpointIds,
                    onNavigateToIrrigationConfig = onNavigateToIrrigationConfig,
                    onSelectSector = onSelectSector,
                    onDeviceClick = onDeviceClick,
                    onSetpointValueChange = onSetpointValueChange,
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
    savingSetpointIds: Set<Long> = emptySet(),
    onNavigateToIrrigationConfig: (Long) -> Unit,
    onSelectSector: (Int) -> Unit,
    onDeviceClick: (String) -> Unit = {},
    onSetpointValueChange: (setpointId: Long, newValue: String) -> Unit = { _, _ -> },
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
                        DeviceCard(
                            device = device,
                            modifier = Modifier.weight(1f),
                            onClick = { onDeviceClick(device.code) })
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

        // Setpoints section
        val setpointCount = selectedSector?.setpoints?.size ?: 0

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title = "Consignas")
            if (setpointCount > 0) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = setpointCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (selectedSector != null && selectedSector.setpoints.isNotEmpty()) {
            selectedSector.setpoints.forEach { setpoint ->
                SetpointCard(
                    setpoint = setpoint,
                    isSaving = savingSetpointIds.contains(setpoint.id),
                    onValueChange = { newValue ->
                        onSetpointValueChange(setpoint.id, newValue)
                    },
                )
            }
        } else {
            Text(
                text = "Sin consignas en este sector",
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
    onClick: () -> Unit = {},
) {
    val statusColor = when {
        device.currentValue == null -> Color(0xFF666666)
        !device.isActive -> Color(0xFFEF5350)
        else -> Color(0xFF4CAF50)
    }

    val displayValue = formatDeviceValue(device.currentValue, device.unitSymbol)

    Card(
        onClick = onClick,
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
private fun SetpointCard(
    setpoint: Setpoint,
    isSaving: Boolean = false,
    onValueChange: (String) -> Unit = {},
) {
    val statusColor = if (setpoint.isActive) Color(0xFF4CAF50) else Color(0xFF666666)
    val isBoolean = setpoint.dataTypeName?.uppercase() == "BOOLEAN"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Top row: info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Code + parameter info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setpoint.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = setpoint.parameterName ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Actuator state chip
                setpoint.actuatorStateName?.let { state ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = state,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Saving indicator
                if (isSaving) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value editor based on data type
            if (isBoolean) {
                SetpointBooleanEditor(
                    currentValue = setpoint.currentValue,
                    enabled = !isSaving,
                    onValueChange = onValueChange,
                )
            } else {
                SetpointTextEditor(
                    currentValue = setpoint.currentValue,
                    dataTypeName = setpoint.dataTypeName,
                    enabled = !isSaving,
                    onValueChange = onValueChange,
                )
            }
        }
    }
}

@Composable
private fun SetpointBooleanEditor(
    currentValue: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val isChecked = currentValue?.lowercase() == "true"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (isChecked) "ON" else "OFF",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isChecked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(
            checked = isChecked,
            onCheckedChange = { newChecked ->
                onValueChange(newChecked.toString())
            },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
private fun SetpointTextEditor(
    currentValue: String?,
    dataTypeName: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var editValue by remember(currentValue) {
        mutableStateOf(currentValue ?: "")
    }
    val hasChanges = editValue != (currentValue ?: "")

    val keyboardType = when (dataTypeName?.uppercase()) {
        "INTEGER", "INT" -> KeyboardType.Number
        "DOUBLE", "FLOAT", "REAL", "DECIMAL" -> KeyboardType.Decimal
        else -> KeyboardType.Text
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = editValue,
            onValueChange = { newText ->
                // Validate input based on data type
                val filtered = when (dataTypeName?.uppercase()) {
                    "INTEGER", "INT" -> newText.filter { it.isDigit() || it == '-' }
                    "DOUBLE", "FLOAT", "REAL", "DECIMAL" -> newText.filter { it.isDigit() || it == '.' || it == '-' }
                    else -> newText
                }
                editValue = filtered
            },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (hasChanges) {
                        onValueChange(editValue)
                    }
                    focusManager.clearFocus()
                },
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(8.dp),
        )

        if (hasChanges) {
            IconButton(
                onClick = {
                    onValueChange(editValue)
                    focusManager.clearFocus()
                },
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Guardar",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
                    SectorWithDevices(
                        2L, "SEC-00002", "Sector B", devices = emptyList(),
                        setpoints = listOf(
                            Setpoint(
                                79L, "SET-00079", "Irrigator setpoint",
                                "IRRIGATOR", "REAL_TEST", "REAL",
                                null, null, true, null,
                            ),
                            Setpoint(
                                80L, "SET-00080", "Irrigator boolean",
                                "IRRIGATOR", "BOOLEAN_TEST", "BOOLEAN",
                                "true", "true", true, null,
                            ),
                        ),
                    ),
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
