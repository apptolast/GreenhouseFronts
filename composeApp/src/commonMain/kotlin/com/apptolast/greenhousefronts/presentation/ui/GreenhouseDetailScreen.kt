package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Greenhouse detail screen (stateful).
 */
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GreenhouseDetailContent(
    uiState: GreenhouseDetailUiState,
    onNavigateBack: () -> Unit,
    onToggleActive: () -> Unit,
    onNavigateToIrrigationConfig: (Long) -> Unit,
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
                                color = if (greenhouse.isActive) {
                                    Color(0xFF4CAF50)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
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
                    onNavigateToIrrigationConfig = onNavigateToIrrigationConfig,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GreenhouseDetailBody(
    greenhouse: Greenhouse,
    modifier: Modifier = Modifier,
    onNavigateToIrrigationConfig: (Long) -> Unit,
) {
    Column(
        modifier = modifier
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
                StatCard(
                    value = formatted,
                    label = "Área",
                    modifier = Modifier.weight(1f),
                )
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

        // Configuración de Riego card
        IrrigationConfigCard(
            onClick = { onNavigateToIrrigationConfig(greenhouse.id) },
        )

        // Sector chips
        if (greenhouse.sectorNames.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                greenhouse.sectorNames.forEach { name ->
                    SectorChip(name = name)
                }
            }
        }

        // Dispositivos section (empty)
        SectionTitle(title = "Dispositivos")

        // Actuadores section (empty)
        SectionTitle(title = "Actuadores")

        Spacer(modifier = Modifier.height(16.dp))
    }
}

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
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
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
private fun SectorChip(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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

@Preview
@Composable
private fun PreviewGreenhouseDetail() {
    GreenhouseTheme(darkTheme = true) {
        GreenhouseDetailContent(
            uiState = GreenhouseDetailUiState(
                isLoading = false,
                greenhouse = Greenhouse(
                    id = 1L,
                    code = "GRH-00001",
                    name = "Invernadero Norte",
                    isActive = true,
                    areaM2 = 2500.0,
                    sectorCount = 3,
                    alertCount = 3,
                    sectorNames = listOf("Sector A", "Sector B", "Sector C"),
                ),
            ),
            onNavigateBack = {},
            onToggleActive = {},
            onNavigateToIrrigationConfig = {},
        )
    }
}

@Preview
@Composable
private fun PreviewGreenhouseDetailInactive() {
    GreenhouseTheme(darkTheme = true) {
        GreenhouseDetailContent(
            uiState = GreenhouseDetailUiState(
                isLoading = false,
                greenhouse = Greenhouse(
                    id = 2L,
                    code = "GRH-00002",
                    name = "Invernadero Este",
                    isActive = false,
                    areaM2 = 3200.0,
                    sectorCount = 5,
                    alertCount = 0,
                    sectorNames = listOf("Sector A", "Sector B", "Sector C", "Sector D", "Sector E"),
                ),
            ),
            onNavigateBack = {},
            onToggleActive = {},
            onNavigateToIrrigationConfig = {},
        )
    }
}
