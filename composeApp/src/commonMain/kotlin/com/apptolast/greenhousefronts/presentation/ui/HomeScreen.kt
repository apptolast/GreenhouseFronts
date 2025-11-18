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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import com.apptolast.greenhousefronts.util.GreenhouseConstants
import com.apptolast.greenhousefronts.util.formatDecimals
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.actuator_sector_label
import greenhousefronts.composeapp.generated.resources.actuator_ventilation_label
import greenhousefronts.composeapp.generated.resources.empty_state
import greenhousefronts.composeapp.generated.resources.greenhouse_number
import greenhousefronts.composeapp.generated.resources.home_actuators_section_title
import greenhousefronts.composeapp.generated.resources.home_monitoring_section_title
import greenhousefronts.composeapp.generated.resources.home_title
import greenhousefronts.composeapp.generated.resources.sensor_humidity_label
import greenhousefronts.composeapp.generated.resources.sensor_temperature_label
import greenhousefronts.composeapp.generated.resources.status_closed
import greenhousefronts.composeapp.generated.resources.status_open
import greenhousefronts.composeapp.generated.resources.success_changes_saved
import org.jetbrains.compose.resources.stringResource

/**
 * Home screen displaying greenhouse sensor data with a modern UI design.
 * Shows 3 greenhouses with tabs for navigation and detailed sensor/actuator controls.
 *
 * @param viewModel The GreenhouseViewModel managing the state and business logic
 * @param onNavigateToSensorDetail Callback for navigating to sensor detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GreenhouseViewModel,
    onNavigateToSensorDetail: (greenhouseId: String, sensorType: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStatsDialog by remember { mutableStateOf(false) }

    // Get localized strings
    val successMessage = stringResource(Res.string.success_changes_saved)

    // Show error or success messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.publishSuccess) {
        if (uiState.publishSuccess) {
            snackbarHostState.showSnackbar(successMessage)
            viewModel.clearPublishSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.home_title),
                        fontWeight = FontWeight.Bold
                    )
                },
//                navigationIcon = {
//                    IconButton(onClick = { /* TODO: Open menu */ }) {
//                        Icon(Icons.Default.Menu, contentDescription = "Menu")
//                    }
//                },
                actions = {
//                    IconButton(onClick = { /* TODO: Open notifications */ }) {
//                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
//                    }
                    // WebSocket status indicator (moved below tabs)
                    WebSocketStatusIndicator(
                        isConnected = uiState.webSocketState.isConnected,
                        dataSource = uiState.dataSource,
                        onStatusClick = { showStatsDialog = true },
                        modifier = Modifier.padding(8.dp)
                    )
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
            // Tabs for greenhouse selection
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedGreenhouseId - 1
            ) {
                repeat(3) { index ->
                    Tab(
                        selected = uiState.selectedGreenhouseId == index + 1,
                        onClick = { viewModel.selectGreenhouse(index + 1) },
                        text = { Text(stringResource(Res.string.greenhouse_number, index + 1)) }
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
                        stringResource(Res.string.home_monitoring_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SensorCards(
                        greenhouse = selectedGreenhouse,
                        onNavigateToSensorDetail = onNavigateToSensorDetail
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actuators Section
                    Text(
                        stringResource(Res.string.home_actuators_section_title),
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
                        stringResource(Res.string.empty_state),
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
private fun SensorCards(
    greenhouse: GreenhouseData,
    onNavigateToSensorDetail: (greenhouseId: String, sensorType: String) -> Unit
) {
    // Get real greenhouse UUID from backend mapping
    val greenhouseUuid = GreenhouseConstants.getGreenhouseUuid(greenhouse.id)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Temperature Card
        SensorCard(
            title = stringResource(Res.string.sensor_temperature_label),
            value = greenhouse.temperatura?.let { "${it.formatDecimals(1)}°C" } ?: "-- °C",
            icon = "🌡️",
            color = Color(0xFFFF6B6B),
            onClick = { onNavigateToSensorDetail(greenhouseUuid, "TEMPERATURE") },
            modifier = Modifier.weight(1f)
        )

        // Humidity Card
        SensorCard(
            title = stringResource(Res.string.sensor_humidity_label),
            value = greenhouse.humedad?.let { "${it.formatDecimals(0)}%" } ?: "-- %",
            icon = "💧",
            color = Color(0xFF4ECDC4),
            onClick = { onNavigateToSensorDetail(greenhouseUuid, "HUMIDITY") },
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
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        onClick = onClick,
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
    onSectorChange: (Int, Double) -> Unit,
    onExtractorChange: (Double) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sector sliders (irrigation valves)
        greenhouse.sectores.forEachIndexed { index, value ->
            SectorSlider(
                label = stringResource(Res.string.actuator_sector_label, index + 1),
                value = value ?: 0.0,
                onValueChange = { newValue ->
                    onSectorChange(index, newValue)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Extractor (ventilation) toggle
        ExtractorToggle(
            isOn = greenhouse.extractor == 1.0,
            onToggle = { isOn ->
                onExtractorChange(if (isOn) 1.0 else 0.0)
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
    value: Double,
    onValueChange: (Double) -> Unit
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
                    onValueChange(sliderValue.toDouble())
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
                            stringResource(Res.string.actuator_ventilation_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isOn) stringResource(Res.string.status_open) else stringResource(Res.string.status_closed),
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
