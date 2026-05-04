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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.model.AlertActor
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.AlertTransition
import com.apptolast.greenhousefronts.presentation.ui.components.EmptyState
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
import com.apptolast.greenhousefronts.presentation.ui.components.formatTimestamp
import com.apptolast.greenhousefronts.presentation.ui.components.parseHex
import com.apptolast.greenhousefronts.presentation.viewmodel.AlertsTab
import com.apptolast.greenhousefronts.presentation.viewmodel.AlertsViewModel

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
        // Nested inside MainScreen's Scaffold which already consumes system bar insets.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AlertsTabRow(selected = state.tab, onSelected = viewModel::selectTab)
            SeverityFilterRow(selected = state.severityFilter, onToggle = viewModel::toggleSeverity)

            val isLoading = when (state.tab) {
                AlertsTab.ACTIVE -> state.activeIsLoading
                AlertsTab.HISTORY -> state.historyIsLoading
            }
            LoadingBar(isLoading = isLoading)

            when (state.tab) {
                AlertsTab.ACTIVE -> ActiveAlertsContent(
                    alerts = state.activeAlerts,
                    severityFilter = state.severityFilter,
                    expandedId = state.expandedActiveId,
                    isLoading = state.activeIsLoading,
                    onItemClick = viewModel::toggleExpandActive,
                )

                AlertsTab.HISTORY -> HistoryTransitionsContent(
                    transitions = state.historyTransitions,
                    severityFilter = state.severityFilter,
                    expandedId = state.expandedTransitionId,
                    isLoading = state.historyIsLoading,
                    isAppending = state.historyIsAppending,
                    hasMore = state.historyHasMore,
                    appendError = state.historyAppendError,
                    onItemClick = viewModel::toggleExpandTransition,
                    onRefresh = viewModel::refreshHistory,
                    onLoadMore = viewModel::loadNextHistoryPage,
                )
            }
        }
    }
}

// ───────── Tabs / filters ─────────

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

// ───────── Active tab ─────────

@Composable
private fun ActiveAlertsContent(
    alerts: List<Alert>,
    severityFilter: Set<AlertSeverity>,
    expandedId: Long?,
    isLoading: Boolean,
    onItemClick: (Long) -> Unit,
) {
    val visible = remember(alerts, severityFilter) {
        alerts.filter { it.severity in severityFilter }
    }
    when {
        severityFilter.isEmpty() && !isLoading ->
            EmptyState(message = "Selecciona al menos una severidad")
        alerts.isEmpty() && !isLoading -> EmptyState(message = "No hay alertas activas")
        visible.isEmpty() && !isLoading -> EmptyState(message = "Ninguna alerta coincide con los filtros")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visible, key = { it.id }) { alert ->
                ActiveAlertItem(
                    alert = alert,
                    expanded = alert.id == expandedId,
                    onClick = { onItemClick(alert.id) },
                )
            }
        }
    }
}

@Composable
private fun ActiveAlertItem(
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
            ItemHeader(
                code = alert.code,
                severity = alert.severity,
                severityColor = severityColor,
                trailingTimestamp = formatTimestamp(alert.createdAt),
            )

            val summary = alert.clientName ?: alert.message ?: alert.description ?: "Sin descripción"
            Spacer(Modifier.height(6.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
            )

            Spacer(Modifier.height(6.dp))
            StatusLine(text = "Activa", color = severityColor)

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ActiveAlertDetails(alert)
            }
        }
    }
}

@Composable
private fun ActiveAlertDetails(alert: Alert) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(12.dp))
        DetailRow("Sector", alert.sectorCode ?: "—")
        alert.alertTypeName?.let { DetailRow("Tipo", it) }
        alert.message?.takeIf { it.isNotBlank() }?.let { DetailRow("Mensaje", it) }
        alert.description?.takeIf { it.isNotBlank() }?.let { DetailRow("Descripción", it) }
        DetailRow("Creada", formatTimestamp(alert.createdAt))
    }
}

// ───────── History tab ─────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTransitionsContent(
    transitions: List<AlertTransition>,
    severityFilter: Set<AlertSeverity>,
    expandedId: Long?,
    isLoading: Boolean,
    isAppending: Boolean,
    hasMore: Boolean,
    appendError: String?,
    onItemClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        // Bind only to first-page loading; appending must not trigger the pull spinner.
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isLoading,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        when {
            severityFilter.isEmpty() ->
                EmptyState(message = "Selecciona al menos una severidad")

            transitions.isEmpty() && !isLoading ->
                EmptyState(message = "Sin transiciones en el histórico")

            else -> {
                val listState = rememberLazyListState()
                // Auto-load when the user gets within 5 items of the bottom. The
                // LaunchedEffect re-fires whenever any of the inputs change, but the VM
                // guards against re-entry.
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val total = info.totalItemsCount
                        if (total == 0) false
                        else {
                            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisible >= total - LOAD_MORE_THRESHOLD
                        }
                    }
                }
                LaunchedEffect(shouldLoadMore, hasMore, isAppending, isLoading) {
                    if (shouldLoadMore && hasMore && !isAppending && !isLoading) {
                        onLoadMore()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(transitions, key = { it.transitionId }) { tx ->
                        TransitionItem(
                            transition = tx,
                            expanded = tx.transitionId == expandedId,
                            onClick = { onItemClick(tx.transitionId) },
                        )
                    }
                    // Footer: spinner / retry button / fallback "Cargar más" / nothing.
                    if (isAppending) {
                        item { AppendingFooter() }
                    } else if (appendError != null) {
                        item { AppendErrorFooter(message = appendError, onRetry = onLoadMore) }
                    } else if (hasMore && !isLoading) {
                        // Fallback for tiny lists where the auto-load watcher doesn't trip.
                        item { LoadMoreFooter(onClick = onLoadMore) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitionItem(
    transition: AlertTransition,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val severityColor = parseHex(transition.severity.colorHex)
    val statusText = if (transition.toResolved) "Resuelta" else "Abierta"
    val statusColor = if (transition.toResolved) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        severityColor
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ItemHeader(
                code = transition.alertCode,
                severity = transition.severity,
                severityColor = severityColor,
                trailingTimestamp = formatTimestamp(transition.at),
            )

            val summary = transition.alertMessage?.takeIf { it.isNotBlank() } ?: "Sin descripción"
            Spacer(Modifier.height(6.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
            )

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusLine(text = statusText, color = statusColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "· ${transition.actor.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                TransitionDetails(transition)
            }
        }
    }
}

@Composable
private fun TransitionDetails(transition: AlertTransition) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(12.dp))
        DetailRow("Sector", transition.sectorCode ?: "—")
        transition.alertTypeName?.let { DetailRow("Tipo", it) }
        DetailRow("Origen", sourceLabel(transition.source))
        DetailRow("Actor", actorDetail(transition.actor))
        DetailRow("Cuándo", formatTimestamp(transition.at))
        if (transition.occurrenceNumber > 0) {
            DetailRow("Aparición #", transition.occurrenceNumber.toString())
        }
    }
}

private fun sourceLabel(source: String): String = when (source.uppercase()) {
    "MQTT" -> "Sensor (MQTT)"
    "API" -> "Acción manual (API)"
    "SYSTEM" -> "Sistema"
    else -> source
}

private fun actorDetail(actor: AlertActor): String = when (actor.kind) {
    AlertActor.ActorKind.USER -> {
        val name = actor.displayName ?: actor.username ?: "Usuario"
        actor.username?.takeIf { it.isNotBlank() && it != name }?.let { "$name (@$it)" } ?: name
    }

    AlertActor.ActorKind.DEVICE -> actor.ref?.let { "Sensor $it" } ?: "Sensor"
    AlertActor.ActorKind.SYSTEM -> "Sistema (automático)"
    AlertActor.ActorKind.UNKNOWN -> "—"
}

// ───────── Shared bits ─────────

@Composable
private fun ItemHeader(
    code: String,
    severity: AlertSeverity,
    severityColor: Color,
    trailingTimestamp: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(10.dp).background(severityColor, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        SeverityBadge(severity)
        Spacer(Modifier.weight(1f))
        Text(
            text = trailingTimestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusLine(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
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

@Composable
private fun AppendingFooter() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun AppendErrorFooter(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun LoadMoreFooter(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onClick) {
            Text("Cargar más")
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 5
