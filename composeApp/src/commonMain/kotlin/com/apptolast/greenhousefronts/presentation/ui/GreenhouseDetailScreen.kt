package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.Device
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.domain.model.SectorWithDevices
import com.apptolast.greenhousefronts.domain.model.Setpoint
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailViewModel
import com.apptolast.greenhousefronts.util.formatDeviceValue
import com.apptolast.greenhousefronts.util.isTrueLike
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GreenhouseDetailScreen(
    greenhouseId: Long,
    viewModel: GreenhouseDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToIrrigationConfig: (Long) -> Unit,
    onNavigateToDeviceDetail: (String) -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
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
        onSendSetpointCommand = viewModel::sendSetpointCommand,
        onNavigateToAlerts = onNavigateToAlerts,
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
    onSendSetpointCommand: (code: String, newValue: String) -> Unit = { _, _ -> },
    onNavigateToAlerts: () -> Unit = {},
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
                    savingSetpointCodes = uiState.savingSetpointCodes,
                    onNavigateToIrrigationConfig = onNavigateToIrrigationConfig,
                    onSelectSector = onSelectSector,
                    onDeviceClick = onDeviceClick,
                    onSendSetpointCommand = onSendSetpointCommand,
                    onNavigateToAlerts = onNavigateToAlerts,
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
    savingSetpointCodes: Set<String> = emptySet(),
    onNavigateToIrrigationConfig: (Long) -> Unit,
    onSelectSector: (Int) -> Unit,
    onDeviceClick: (String) -> Unit = {},
    onSendSetpointCommand: (code: String, newValue: String) -> Unit = { _, _ -> },
    onNavigateToAlerts: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Active alerts card (clickable, navigates to alerts screen)
        if (greenhouse.alertCount > 0) {
            ActiveAlertsCard(
                count = greenhouse.alertCount,
                onClick = onNavigateToAlerts,
            )
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
            val chunkedSetpoints = selectedSector.setpoints.chunked(2)
            chunkedSetpoints.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { setpoint ->
                        SetpointCard(
                            setpoint = setpoint,
                            isSaving = savingSetpointCodes.contains(setpoint.code),
                            onValueChange = { newValue ->
                                onSendSetpointCommand(setpoint.code, newValue)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
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

    val displayValue = formatDeviceValue(device.currentValue, device.dataType)

    // Flash the card background briefly whenever the displayed value changes.
    // We drive the colour with a single Animatable so we can use an asymmetric profile:
    // a fast rise (≈120 ms) followed by a slow decay (≈900 ms). This is perceived as ONE
    // pulse — a symmetric tween on both directions feels like two separate events
    // (background going green, then going back). We also skip the very first composition
    // so the cards don't all flash on screen entry.
    val surfaceColor = MaterialTheme.colorScheme.surface
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val cardColor = remember { Animatable(surfaceColor) }
    var hasComposed by remember { mutableStateOf(false) }
    LaunchedEffect(displayValue) {
        if (!hasComposed) {
            hasComposed = true
            return@LaunchedEffect
        }
        cardColor.snapTo(highlightColor)
        cardColor.animateTo(
            targetValue = surfaceColor,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor.value),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: icon area + status dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
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

            // Device name (prefer clientName for user-friendly display)
            Text(
                text = device.clientName ?: device.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Value + unit — value slides in from below on each change.
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AnimatedContent(
                    targetState = displayValue,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ) { it / 2 } + fadeIn(tween(250))) togetherWith
                                (slideOutVertically(
                                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                                ) { -it / 2 } + fadeOut(tween(150)))
                    },
                    label = "deviceValue",
                ) { animatedValue ->
                    Text(
                        text = animatedValue,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    onValueChange: (String) -> Unit = {},
) {
    val statusColor = if (setpoint.isActive) Color(0xFF4CAF50) else Color(0xFF666666)
    val isBoolean = setpoint.dataTypeName?.uppercase() == "BOOLEAN"

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: actuator state chip + status dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                setpoint.actuatorStateName?.let { state ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = state,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } ?: Spacer(modifier = Modifier.size(1.dp))

                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name (prefer clientName for user-friendly display)
            Text(
                text = setpoint.clientName ?: setpoint.parameterName ?: setpoint.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Value editor
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
    val isChecked = currentValue.isTrueLike()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (isChecked) "ON" else "OFF",
            style = MaterialTheme.typography.headlineSmall,
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = editValue,
            onValueChange = { newText ->
                val filtered = when (dataTypeName?.uppercase()) {
                    "INTEGER", "INT" -> newText.filter { it.isDigit() || it == '-' }
                    "DOUBLE", "FLOAT", "REAL", "DECIMAL" -> newText.filter { it.isDigit() || it == '.' || it == '-' }
                    else -> newText
                }
                editValue = filtered
            },
            modifier = Modifier.weight(1f).height(48.dp),
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
                    if (hasChanges) onValueChange(editValue)
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
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enviar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveAlertsCard(count: Int, onClick: () -> Unit) {
    val errorColor = MaterialTheme.colorScheme.error
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, errorColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = errorColor.copy(alpha = 0.15f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = errorColor,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "$count ${if (count == 1) "alerta activa" else "alertas activas"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = errorColor,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = errorColor,
            )
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
                                id = 1L,
                                code = "DEV-001",
                                name = "WS90_TEMP_INTERIOR",
                                clientName = "Temperatura interior",
                                isActive = true,
                                categoryName = "SENSOR",
                                typeName = "TEMPERATURE",
                                typeId = 1,
                                unitSymbol = "°C",
                                currentValue = "23.5",
                                lastUpdated = null,
                                minExpectedValue = -50.0,
                                maxExpectedValue = 100.0,
                                controlType = null,
                            ),
                            Device(
                                id = 2L,
                                code = "DEV-002",
                                name = "WS90_HUM_INTERIOR",
                                clientName = "Humedad relativa",
                                isActive = true,
                                categoryName = "SENSOR",
                                typeName = "HUMIDITY",
                                typeId = 2,
                                unitSymbol = "%",
                                currentValue = "65",
                                lastUpdated = null,
                                minExpectedValue = 0.0,
                                maxExpectedValue = 100.0,
                                controlType = null,
                            ),
                            Device(
                                id = 3L,
                                code = "DEV-003",
                                name = "CO2_SENSOR_01",
                                clientName = "Nivel de CO2",
                                isActive = true,
                                categoryName = "SENSOR",
                                typeName = "CO2 LEVEL",
                                typeId = 5,
                                unitSymbol = "ppm",
                                currentValue = "820",
                                lastUpdated = null,
                                minExpectedValue = 0.0,
                                maxExpectedValue = 5000.0,
                                controlType = null,
                            ),
                            Device(
                                id = 4L,
                                code = "DEV-004",
                                name = "EXTRACTOR_01",
                                clientName = "Extractor principal",
                                isActive = true,
                                categoryName = "ACTUATOR",
                                typeName = "EXTRACTOR",
                                typeId = 24,
                                unitSymbol = "%",
                                currentValue = "75",
                                lastUpdated = null,
                                minExpectedValue = null,
                                maxExpectedValue = null,
                                controlType = "CONTINUOUS",
                            ),
                        ),
                    ),
                    SectorWithDevices(
                        2L, "SEC-00002", "Sector B", devices = emptyList(),
                        setpoints = listOf(
                            Setpoint(
                                id = 79L,
                                code = "SET-00079",
                                clientName = "Consigna de riego",
                                description = "Irrigator setpoint",
                                parameterName = "IRRIGATOR",
                                actuatorStateName = "REAL_TEST",
                                dataTypeName = "REAL",
                                currentValue = null,
                                configuredValue = null,
                                isActive = true,
                                lastUpdated = null,
                            ),
                            Setpoint(
                                id = 80L,
                                code = "SET-00080",
                                clientName = "Activar riego",
                                description = "Irrigator boolean",
                                parameterName = "IRRIGATOR",
                                actuatorStateName = "BOOLEAN_TEST",
                                dataTypeName = "BOOLEAN",
                                currentValue = "true",
                                configuredValue = "true",
                                isActive = true,
                                lastUpdated = null,
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
