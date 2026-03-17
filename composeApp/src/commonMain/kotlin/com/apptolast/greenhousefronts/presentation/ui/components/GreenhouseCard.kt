package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ActiveBorderColor = Color(0xFF4CAF50)
private val InactiveBorderColor = Color(0xFF444444)

@Composable
fun GreenhouseCard(
    greenhouse: Greenhouse,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val borderColor = if (greenhouse.isActive) ActiveBorderColor else InactiveBorderColor

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
        ) {
            Text(
                text = greenhouse.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details as single text to avoid overflow in grid
            Text(
                text = buildDetailText(greenhouse),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2,
            )
        }
    }
}

private fun buildDetailText(greenhouse: Greenhouse): String {
    val parts = mutableListOf<String>()
    parts.add("${greenhouse.sectorCount} ${if (greenhouse.sectorCount == 1) "sector" else "sectores"}")
    greenhouse.areaM2?.let { area ->
        val formatted = if (area == area.toLong().toDouble()) "${area.toLong()} m²" else "$area m²"
        parts.add(formatted)
    }
    if (greenhouse.alertCount > 0) {
        parts.add("⚠ ${greenhouse.alertCount} ${if (greenhouse.alertCount == 1) "alerta" else "alertas"}")
    }
    return parts.joinToString(" · ")
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
