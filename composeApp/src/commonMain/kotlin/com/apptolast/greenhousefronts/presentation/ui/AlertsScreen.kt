package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
import com.apptolast.greenhousefronts.presentation.viewmodel.AlertsTab
import com.apptolast.greenhousefronts.presentation.viewmodel.AlertsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AlertsTabRow(
                selected = state.tab,
                onSelected = viewModel::selectTab,
            )

            SeverityFilterRow(
                selected = state.severityFilter,
                onToggle = viewModel::toggleSeverity,
            )

            LoadingBar(isLoading = state.isLoading)

            val visibleAlerts = remember(state.alerts, state.severityFilter) {
                state.alerts.filter { it.severity in state.severityFilter }
            }

            when {
                state.alerts.isEmpty() && !state.isLoading -> AlertsEmptyState(state.tab)
                visibleAlerts.isEmpty() && !state.isLoading -> AlertsEmptyState(
                    tab = state.tab,
                    overrideMessage = "Ninguna alerta coincide con los filtros seleccionados",
                )

                else -> AlertsList(
                    alerts = visibleAlerts,
                    expandedId = state.expandedAlertId,
                    truncated = state.truncated,
                    onItemClick = viewModel::toggleExpand,
                )
            }
        }
    }
}

@Composable
private fun AlertsTabRow(
    selected: AlertsTab,
    onSelected: (AlertsTab) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AlertsTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AlertsTab.entries.size),
            ) {
                Text(if (tab == AlertsTab.ACTIVE) "Activas" else "Histórico")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeverityFilterRow(
    selected: Set<AlertSeverity>,
    onToggle: (AlertSeverity) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlertSeverity.entries.forEach { severity ->
            val isSelected = severity in selected
            val severityColor = parseHex(severity.colorHex)
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(severity) },
                label = { Text(severity.display) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(severityColor, CircleShape),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = severityColor.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun AlertsEmptyState(
    tab: AlertsTab,
    overrideMessage: String? = null,
) {
    val message = overrideMessage ?: when (tab) {
        AlertsTab.ACTIVE -> "No hay alertas activas"
        AlertsTab.HISTORY -> "Sin alertas en el histórico"
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlertsList(
    alerts: List<Alert>,
    expandedId: Long?,
    truncated: Boolean,
    onItemClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(alerts, key = { it.id }) { alert ->
            AlertItem(
                alert = alert,
                expanded = alert.id == expandedId,
                onClick = { onItemClick(alert.id) },
            )
        }
        if (truncated) {
            item {
                Text(
                    text = "Mostrando las últimas 100 alertas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AlertItem(
    alert: Alert,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val severityColor = parseHex(alert.severity.colorHex)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: severity dot + code · severity chip + status + date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(severityColor, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = alert.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                SeverityBadge(alert.severity)
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatTimestamp(alert.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Single-line summary (clientName ?? message ?? "Sin descripción")
            val summary = alert.clientName ?: alert.message ?: alert.description ?: "Sin descripción"
            Spacer(Modifier.height(6.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
            )

            // Status row
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (alert.isResolved) "Resuelta" else "Activa",
                style = MaterialTheme.typography.labelMedium,
                color = if (alert.isResolved) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    severityColor
                },
            )

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                AlertDetails(alert)
            }
        }
    }
}

@Composable
private fun AlertDetails(alert: Alert) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(12.dp))

        DetailRow(label = "Sector", value = alert.sectorCode ?: "—")
        alert.alertTypeName?.let { DetailRow(label = "Tipo", value = it) }
        alert.message?.takeIf { it.isNotBlank() }?.let { DetailRow(label = "Mensaje", value = it) }
        alert.description?.takeIf { it.isNotBlank() }?.let { DetailRow(label = "Descripción", value = it) }
        DetailRow(label = "Creada", value = formatTimestamp(alert.createdAt))

        if (alert.isResolved) {
            val resolvedSuffix = alert.resolvedByUserName?.let { " · $it" } ?: ""
            DetailRow(
                label = "Resuelta",
                value = (alert.resolvedAt?.let { formatTimestamp(it) } ?: "—") + resolvedSuffix,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label:",
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SeverityBadge(severity: AlertSeverity) {
    val color = parseHex(severity.colorHex)
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = severity.display,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Multiplatform-safe hex parser. Compose's `Color(android.graphics.Color.parseColor(...))`
 * isn't available in commonMain; we handle the `#RRGGBB` shape manually.
 */
private fun parseHex(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    return runCatching {
        val rgb = cleaned.toLong(16)
        Color(0xFF000000 or rgb)
    }.getOrDefault(Color.Gray)
}

/**
 * `2026-04-30T07:14:23.123Z` → `2026-04-30 07:14`. Wrong-format strings round-trip
 * unchanged so we never crash the screen on a backend timestamp the parser doesn't like.
 */
private fun formatTimestamp(raw: String): String {
    if (raw.isBlank()) return "—"
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.year}-$month-$day $hour:$minute"
}
