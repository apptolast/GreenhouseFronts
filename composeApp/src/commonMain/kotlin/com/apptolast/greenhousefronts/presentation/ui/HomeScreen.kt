package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import com.apptolast.greenhousefronts.util.formatDecimals
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.data.model.GreenhouseData
import com.apptolast.greenhousefronts.presentation.ui.components.ConnectionStatsDialog
import com.apptolast.greenhousefronts.presentation.ui.components.WebSocketStatusIndicator
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseViewModel

/**
 * Home screen displaying greenhouse sensor data with a modern UI design.
 * Shows 3 greenhouses with tabs for navigation and detailed sensor/actuator controls.
 *
 * @param viewModel The GreenhouseViewModel managing the state and business logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: GreenhouseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStatsDialog by remember { mutableStateOf(false) }

    // Show error or success messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.publishSuccess) {
        if (uiState.publishSuccess) {
            snackbarHostState.showSnackbar("Cambios guardados exitosamente")
            viewModel.clearPublishSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Invernadero", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open menu */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Open notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // WebSocket status indicator
            WebSocketStatusIndicator(
                isConnected = uiState.webSocketState.isConnected,
                dataSource = uiState.dataSource,
                onStatusClick = { showStatsDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tabs for greenhouse selection
            TabRow(
                selectedTabIndex = uiState.selectedGreenhouseId - 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                repeat(3) { index ->
                    Tab(
                        selected = uiState.selectedGreenhouseId == index + 1,
                        onClick = { viewModel.selectGreenhouse(index + 1) },
                        text = { Text("Invernadero ${index + 1}") }
                    )
                }
            }

            // Get the selected greenhouse data
            val selectedGreenhouse =
                uiState.greenhouses.find { it.id == uiState.selectedGreenhouseId }

            if (selectedGreenhouse != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Monitoring Section
                    Text(
                        "Monitoreo de Sensores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SensorCards(greenhouse = selectedGreenhouse)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actuators Section
                    Text(
                        "Control de Actuadores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ActuatorControls(
                        greenhouse = selectedGreenhouse,
                        onSectorChange = { sectorIndex, value ->
                            viewModel.updateSector(selectedGreenhouse.id, sectorIndex, value)
                        },
                        onExtractorChange = { value ->
                            viewModel.updateExtractor(selectedGreenhouse.id, value)
                        }
                    )
                }
            } else {
                // No data available
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay datos disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Connection statistics dialog
    ConnectionStatsDialog(
        isVisible = showStatsDialog,
        connectionState = uiState.webSocketState,
        dataSource = uiState.dataSource,
        onDismiss = { showStatsDialog = false },
        onReconnect = { viewModel.reconnectWebSocket() }
    )
}

/**
 * Sensor monitoring cards showing temperature and humidity
 */
@Composable
private fun SensorCards(greenhouse: GreenhouseData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Temperature Card
        SensorCard(
            title = "Temperatura",
            value = greenhouse.temperatura?.let { "${it.formatDecimals(1)}°C" } ?: "-- °C",
            icon = "🌡️",
            color = Color(0xFFFF6B6B),
            modifier = Modifier.weight(1f)
        )

        // Humidity Card
        SensorCard(
            title = "Humedad",
            value = greenhouse.humedad?.let { "${it.formatDecimals(0)}%" } ?: "-- %",
            icon = "💧",
            color = Color(0xFF4ECDC4),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual sensor card component
 */
@Composable
private fun SensorCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    icon,
                    fontSize = 24.sp
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Actuator controls section with sliders and switches
 */
@Composable
private fun ActuatorControls(
    greenhouse: GreenhouseData,
    onSectorChange: (Int, Int) -> Unit,
    onExtractorChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sector sliders (irrigation valves)
        greenhouse.sectores.forEachIndexed { index, value ->
            SectorSlider(
                label = "Sector ${index + 1}",
                value = value ?: 0,
                onValueChange = { newValue ->
                    onSectorChange(index, newValue)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Extractor (ventilation) toggle
        ExtractorToggle(
            isOn = greenhouse.extractor == 1,
            onToggle = { isOn ->
                onExtractorChange(if (isOn) 1 else 0)
            }
        )
    }
}

/**
 * Slider for sector control
 */
@Composable
private fun SectorSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💧", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "${sliderValue.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ECDC4)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onValueChange(sliderValue.toInt())
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF4ECDC4),
                    activeTrackColor = Color(0xFF4ECDC4),
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}

/**
 * Toggle switch for extractor control
 */
@Composable
private fun ExtractorToggle(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ventilation Toggle
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌀", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Ventilación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isOn) "Abierto" else "Cerrado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOn) Color(0xFF4ECDC4) else MaterialTheme.colorScheme.outline
                    )
                }

                Switch(
                    checked = isOn,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4ECDC4),
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}
