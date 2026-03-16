package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GreenhouseCard(
    greenhouse: Greenhouse,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Top row: Name + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greenhouse.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                StatusBadge(isActive = greenhouse.isActive)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Area · Sectors · Alerts
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                DetailChip(
                    text = "${greenhouse.sectorCount} ${if (greenhouse.sectorCount == 1) "sector" else "sectores"}",
                )

                greenhouse.areaM2?.let { area ->

                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )

                    //TODO: implement the m2 fromat in a string with variables and quantity.
                    val formattedArea = if (area == area.toLong().toDouble()) {
                        "${area.toLong()} m²"
                    } else {
                        "$area m²"
                    }
                    DetailChip(text = formattedArea)
                }

                if (greenhouse.alertCount > 0) {
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    DetailChip(
                        text = "⚠ ${greenhouse.alertCount} ${if (greenhouse.alertCount == 1) "alerta" else "alertas"}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val backgroundColor = if (isActive) {
        Color(0xFF1B3A1B)
    } else {
        Color(0xFF3A1B1B)
    }
    val textColor = if (isActive) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFEF5350)
    }
    val dotColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val label = if (isActive) "Activo" else "Inactivo"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DetailChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
    )
}

@Preview
@Composable
private fun PreviewGreenhouseCardActive() {
    GreenhouseTheme(darkTheme = true) {
        GreenhouseCard(
            greenhouse = Greenhouse(
                id = 1L,
                code = "GRH-00001",
                name = "Invernadero Norte",
                isActive = true,
                areaM2 = 2500.0,
                sectorCount = 4,
                alertCount = 2,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewGreenhouseCardInactive() {
    GreenhouseTheme(darkTheme = true) {
        GreenhouseCard(
            greenhouse = Greenhouse(
                id = 2L,
                code = "GRH-00002",
                name = "Invernadero Este",
                isActive = false,
                areaM2 = 3200.0,
                sectorCount = 5,
                alertCount = 0,
            ),
        )
    }
}
