package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.presentation.viewmodel.DataSource
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.websocket_http_label
import greenhousefronts.composeapp.generated.resources.websocket_realtime_label
import greenhousefronts.composeapp.generated.resources.websocket_status_disconnected
import org.jetbrains.compose.resources.stringResource

/**
 * Status indicator for WebSocket connection
 * Shows connection state and data source (WebSocket vs HTTP)
 *
 * @param isConnected Whether WebSocket is connected
 * @param dataSource Current data source (WebSocket or HTTP)
 * @param onStatusClick Callback when status is clicked (to show stats dialog)
 */
@Composable
fun WebSocketStatusIndicator(
    isConnected: Boolean,
    dataSource: DataSource,
    onStatusClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onStatusClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Status indicator circle
        val indicatorColor = when {
            isConnected && dataSource == DataSource.WEBSOCKET -> Color(0xFF4CAF50) // Green
            !isConnected && dataSource == DataSource.HTTP -> Color(0xFFFFC107) // Amber
            else -> Color(0xFFF44336) // Red
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Status text
        val statusText = when {
            isConnected && dataSource == DataSource.WEBSOCKET -> stringResource(Res.string.websocket_realtime_label)
            dataSource == DataSource.HTTP -> stringResource(Res.string.websocket_http_label)
            else -> stringResource(Res.string.websocket_status_disconnected)
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}