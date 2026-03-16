package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.DayOfWeek
import com.apptolast.greenhousefronts.domain.model.IrrigationConfig
import com.apptolast.greenhousefronts.domain.model.SectorIrrigationConfig
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.IrrigationConfigUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.IrrigationConfigViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Irrigation config screen (stateful).
 */
@Composable
fun IrrigationConfigScreen(
    greenhouseId: Long,
    viewModel: IrrigationConfigViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(greenhouseId) {
        viewModel.loadConfig(greenhouseId)
    }

    IrrigationConfigContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleDay = viewModel::toggleDay,
        onStartHourChanged = viewModel::updateStartHour,
        onStartMinuteChanged = viewModel::updateStartMinute,
        onEndHourChanged = viewModel::updateEndHour,
        onEndMinuteChanged = viewModel::updateEndMinute,
        onWaitBetweenChanged = viewModel::updateWaitBetween,
        onSectorOpeningChanged = viewModel::updateSectorOpening,
        onSectorWaitChanged = viewModel::updateSectorWait,
        onToggleSectorActive = viewModel::toggleSectorActive,
        onSave = viewModel::saveConfig,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IrrigationConfigContent(
    uiState: IrrigationConfigUiState,
    onNavigateBack: () -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onStartHourChanged: (Int) -> Unit,
    onStartMinuteChanged: (Int) -> Unit,
    onEndHourChanged: (Int) -> Unit,
    onEndMinuteChanged: (Int) -> Unit,
    onWaitBetweenChanged: (Int) -> Unit,
    onSectorOpeningChanged: (Int, Int) -> Unit,
    onSectorWaitChanged: (Int, Int) -> Unit,
    onToggleSectorActive: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configuración de Riego",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        uiState.config?.greenhouseName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.config != null -> {
                IrrigationConfigBody(
                    config = uiState.config,
                    isSaving = uiState.isSaving,
                    modifier = Modifier.padding(paddingValues),
                    onToggleDay = onToggleDay,
                    onStartHourChanged = onStartHourChanged,
                    onStartMinuteChanged = onStartMinuteChanged,
                    onEndHourChanged = onEndHourChanged,
                    onEndMinuteChanged = onEndMinuteChanged,
                    onWaitBetweenChanged = onWaitBetweenChanged,
                    onSectorOpeningChanged = onSectorOpeningChanged,
                    onSectorWaitChanged = onSectorWaitChanged,
                    onToggleSectorActive = onToggleSectorActive,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun IrrigationConfigBody(
    config: IrrigationConfig,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    onToggleDay: (DayOfWeek) -> Unit,
    onStartHourChanged: (Int) -> Unit,
    onStartMinuteChanged: (Int) -> Unit,
    onEndHourChanged: (Int) -> Unit,
    onEndMinuteChanged: (Int) -> Unit,
    onWaitBetweenChanged: (Int) -> Unit,
    onSectorOpeningChanged: (Int, Int) -> Unit,
    onSectorWaitChanged: (Int, Int) -> Unit,
    onToggleSectorActive: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Irrigating status banner
        IrrigationStatusBanner(
            isIrrigating = config.isIrrigating,
            statusText = config.irrigationStatus,
        )

        // Manual irrigation toggle (disabled — not yet implemented)
        ManualIrrigationToggle()

        // Weekly schedule
        SectionLabel("Programación semanal")
        WeekDaySelector(
            activeDays = config.activeDays,
            onToggleDay = onToggleDay,
        )

        // Irrigation schedule
        SectionLabel("Horario de riego")
        ScheduleCard(
            config = config,
            onStartHourChanged = onStartHourChanged,
            onStartMinuteChanged = onStartMinuteChanged,
            onEndHourChanged = onEndHourChanged,
            onEndMinuteChanged = onEndMinuteChanged,
            onWaitBetweenChanged = onWaitBetweenChanged,
        )

        // Sector programming
        SectionLabel("Programación por sectores")
        SectorTable(
            sectors = config.sectorConfigs,
            onOpeningChanged = onSectorOpeningChanged,
            onWaitChanged = onSectorWaitChanged,
            onToggleActive = onToggleSectorActive,
        )

        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isSaving) "Guardando..." else "Guardar configuración",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun IrrigationStatusBanner(
    isIrrigating: Boolean,
    statusText: String?,
) {
    val containerColor = if (isIrrigating) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val dotColor = if (isIrrigating) MaterialTheme.colorScheme.primary else Color(0xFF666666)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = if (isIrrigating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isIrrigating) "Regando" else "Sin riego activo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isIrrigating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            statusText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

@Composable
private fun ManualIrrigationToggle() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Riego Manual",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Anular programación automática",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Disabled — not yet implemented
        Switch(
            checked = false,
            onCheckedChange = null,
            enabled = false,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun WeekDaySelector(
    activeDays: Set<DayOfWeek>,
    onToggleDay: (DayOfWeek) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        DayOfWeek.entries.forEach { day ->
            val isActive = day in activeDays
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                        shape = CircleShape,
                    )
                    .clickable { onToggleDay(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.shortLabel,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    config: IrrigationConfig,
    onStartHourChanged: (Int) -> Unit,
    onStartMinuteChanged: (Int) -> Unit,
    onEndHourChanged: (Int) -> Unit,
    onEndMinuteChanged: (Int) -> Unit,
    onWaitBetweenChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TimeRow(
            icon = Icons.Default.Schedule,
            label = "Hora de inicio",
            hour = config.startHour,
            minute = config.startMinute,
            onHourChanged = onStartHourChanged,
            onMinuteChanged = onStartMinuteChanged,
        )

        TimeRow(
            icon = Icons.Default.Schedule,
            label = "Hora de fin",
            hour = config.endHour,
            minute = config.endMinute,
            onHourChanged = onEndHourChanged,
            onMinuteChanged = onEndMinuteChanged,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Espera entre riegos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            NumberInputField(
                value = config.waitBetweenMinutes,
                onValueChanged = onWaitBetweenChanged,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimeRow(
    icon: ImageVector,
    label: String,
    hour: Int,
    minute: Int,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        NumberInputField(value = hour, onValueChanged = onHourChanged)
        Text(
            text = ":",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        NumberInputField(value = minute, onValueChanged = onMinuteChanged)
    }
}

@Composable
private fun NumberInputField(
    value: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value.toString(),
        onValueChange = { text ->
            val num = text.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
            onValueChanged(num)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .width(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 6.dp, horizontal = 4.dp),
    )
}

@Composable
private fun SectorTable(
    sectors: List<SectorIrrigationConfig>,
    onOpeningChanged: (Int, Int) -> Unit,
    onWaitChanged: (Int, Int) -> Unit,
    onToggleActive: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Table header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sector",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.2f),
            )
            Text(
                text = "Apertura\n(min)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                lineHeight = 14.sp,
            )
            Text(
                text = "Espera\n(min)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                lineHeight = 14.sp,
            )
            Text(
                text = "Estado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.7f),
            )
        }

        // Sector rows
        sectors.forEachIndexed { index, sector ->
            SectorRow(
                sector = sector,
                onOpeningChanged = { onOpeningChanged(index, it) },
                onWaitChanged = { onWaitChanged(index, it) },
                onToggleActive = { onToggleActive(index) },
            )
        }
    }
}

@Composable
private fun SectorRow(
    sector: SectorIrrigationConfig,
    onOpeningChanged: (Int) -> Unit,
    onWaitChanged: (Int) -> Unit,
    onToggleActive: () -> Unit,
) {
    val dotColor = if (sector.isActive) MaterialTheme.colorScheme.primary else Color(0xFF666666)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sector name with dot
        Row(
            modifier = Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sector.sectorName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Opening minutes
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            NumberInputField(
                value = sector.openingMinutes,
                onValueChanged = onOpeningChanged,
            )
        }

        // Wait minutes
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            NumberInputField(
                value = sector.waitMinutes,
                onValueChanged = onWaitChanged,
            )
        }

        // Active toggle
        Box(modifier = Modifier.weight(0.7f), contentAlignment = Alignment.Center) {
            Switch(
                checked = sector.isActive,
                onCheckedChange = { onToggleActive() },
                modifier = Modifier.size(width = 40.dp, height = 24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewIrrigationConfig() {
    GreenhouseTheme(darkTheme = true) {
        IrrigationConfigContent(
            uiState = IrrigationConfigUiState(
                isLoading = false,
                config = IrrigationConfig(
                    greenhouseId = 1L,
                    greenhouseName = "Invernadero Norte",
                    isIrrigating = true,
                    irrigationStatus = "Sector A - Válvula abierta",
                    sectorConfigs = listOf(
                        SectorIrrigationConfig(1L, "Sector A", 0, 4, true),
                        SectorIrrigationConfig(2L, "Sector B", 3, 4, false),
                        SectorIrrigationConfig(3L, "Sector C", 2, 4, false),
                        SectorIrrigationConfig(4L, "Sector D", 2, 4, false),
                    ),
                ),
            ),
            onNavigateBack = {},
            onToggleDay = {},
            onStartHourChanged = {},
            onStartMinuteChanged = {},
            onEndHourChanged = {},
            onEndMinuteChanged = {},
            onWaitBetweenChanged = {},
            onSectorOpeningChanged = { _, _ -> },
            onSectorWaitChanged = { _, _ -> },
            onToggleSectorActive = {},
            onSave = {},
        )
    }
}
