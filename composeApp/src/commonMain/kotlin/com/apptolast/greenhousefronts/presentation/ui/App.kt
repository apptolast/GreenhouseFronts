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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apptolast.greenhousefronts.presentation.ui.components.ConnectionStatsDialog
import com.apptolast.greenhousefronts.presentation.ui.components.WebSocketStatusIndicator
import com.apptolast.greenhousefronts.presentation.viewmodel.DataSource
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel: GreenhouseViewModel = viewModel { GreenhouseViewModel() }
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        var userInput by remember { mutableStateOf("") }
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
                snackbarHostState.showSnackbar("Mensaje enviado exitosamente")
                viewModel.clearPublishSuccess()
                userInput = ""
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .safeContentPadding()
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // WebSocket status indicator at the top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebSocketStatusIndicator(
                        isConnected = uiState.webSocketState.isConnected,
                        dataSource = uiState.dataSource,
                        onStatusClick = { showStatsDialog = true }
                    )

                    // Reconnect button (only visible when disconnected)
                    if (!uiState.webSocketState.isConnected && uiState.dataSource == DataSource.HTTP) {
                        OutlinedButton(
                            onClick = { viewModel.reconnectWebSocket() }
                        ) {
                            Text("Reconectar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display data from API
                val displayValue = uiState.lastMessage?.sensor01?.toString()
                    ?: uiState.lastMessage?.setpoint01?.toString()
                    ?: "Sin datos"

                Text(
                    text = "Dato de API: $displayValue",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (uiState.lastMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Greenhouse ID: ${uiState.lastMessage?.greenhouseId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Text field for user input
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Ingresa un valor (setpoint)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Submit button
                Button(
                    onClick = {
                        val value = userInput.toDoubleOrNull()
                        if (value != null) {
                            viewModel.publishSetpoint(value)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && userInput.toDoubleOrNull() != null
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Enviar")
                    }
                }
            }

            // Snackbar host for messages
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
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
}

