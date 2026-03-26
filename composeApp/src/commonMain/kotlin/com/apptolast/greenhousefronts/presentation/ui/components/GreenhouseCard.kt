package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ActiveColor = Color(0xFF4CAF50)
private val InactiveColor = Color(0xFF666666)

@Composable
fun GreenhouseCard(
    greenhouse: Greenhouse,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val borderColor = if (greenhouse.isActive) ActiveColor else InactiveColor

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: name + status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = greenhouse.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (greenhouse.isActive) ActiveColor else InactiveColor),
                    )
                    Text(
                        text = if (greenhouse.isActive) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (greenhouse.isActive) ActiveColor else InactiveColor,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Info rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoChip(label = "Sectores", value = greenhouse.sectorCount.toString())
                greenhouse.areaM2?.let { area ->
                    val formatted =
                        if (area == area.toLong().toDouble()) "${area.toLong()} m\u00B2" else "$area m\u00B2"
                    InfoChip(label = "Superficie", value = formatted)
                }
                if (greenhouse.alertCount > 0) {
                    InfoChip(label = "Alertas", value = greenhouse.alertCount.toString(), highlight = true)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
                sectorNames = listOf("Sector A", "Sector B", "Sector C", "Sector D"),
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
                sectorNames = listOf("Sector 01", "Sector 02"),
            ),
        )
    }
}
