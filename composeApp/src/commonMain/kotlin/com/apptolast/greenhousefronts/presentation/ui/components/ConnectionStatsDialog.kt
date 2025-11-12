package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.data.remote.websocket.WebSocketConnectionState
import com.apptolast.greenhousefronts.presentation.viewmodel.DataSource
import kotlin.math.floor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Dialog showing WebSocket connection statistics
 *
 * @param isVisible Whether the dialog is currently shown
 * @param connectionState Current WebSocket connection state
 * @param dataSource Current data source (WebSocket or HTTP)
 * @param onDismiss Callback when dialog is dismissed
 * @param onReconnect Callback when reconnect button is clicked
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ConnectionStatsDialog(
    isVisible: Boolean,
    connectionState: WebSocketConnectionState,
    dataSource: DataSource,
    onDismiss: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Estadísticas de Conexión",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection status
                StatRow(
                    label = "Estado",
                    value = if (connectionState.isConnected) "Conectado" else "Desconectado",
                    valueColor = if (connectionState.isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                // Data source
                StatRow(
                    label = "Fuente de datos",
                    value = when (dataSource) {
                        DataSource.WEBSOCKET -> "WebSocket (Tiempo real)"
                        DataSource.HTTP -> "HTTP (Polling)"
                    }
                )

                HorizontalDivider()

                // Connection time
                val connectedDuration = connectionState.connectionTime?.let {
                    val durationMs = Clock.System.now().toEpochMilliseconds() - it
                    formatDuration(durationMs)
                } ?: "N/A"

                StatRow(
                    label = "Tiempo conectado",
                    value = if (connectionState.isConnected) connectedDuration else "N/A"
                )

                // Messages received
                StatRow(
                    label = "Mensajes recibidos",
                    value = connectionState.messagesReceived.toString()
                )

                // Reconnect attempts
                if (connectionState.reconnectAttempts > 0) {
                    StatRow(
                        label = "Intentos de reconexión",
                        value = connectionState.reconnectAttempts.toString()
                    )
                }

                // Last error
                connectionState.lastError?.let { error ->
                    HorizontalDivider()
                    Column {
                        Text(
                            text = "Último error:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reconnect button (only show if disconnected)
                if (!connectionState.isConnected && dataSource == DataSource.HTTP) {
                    Button(
                        onClick = {
                            onReconnect()
                            onDismiss()
                        }
                    ) {
                        Text("Reconectar")
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Single row displaying a label-value pair
 */
@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

/**
 * Format milliseconds duration to human-readable string
 */
private fun formatDuration(durationMs: Long): String {
    val seconds = floor(durationMs / 1000.0).toInt()
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}