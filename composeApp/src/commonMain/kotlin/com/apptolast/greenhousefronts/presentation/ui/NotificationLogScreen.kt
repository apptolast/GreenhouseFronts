package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.apptolast.greenhousefronts.data.remote.push.AlertDeepLinkBus
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.NotificationLogEntry
import com.apptolast.greenhousefronts.presentation.ui.components.EmptyState
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
import com.apptolast.greenhousefronts.presentation.ui.components.formatTimestamp
import com.apptolast.greenhousefronts.presentation.ui.components.parseHex
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.NotificationLogUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.NotificationLogViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NotificationLogScreen(
    viewModel: NotificationLogViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    NotificationLogContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onItemClick = ::onEntryTap,
    )
}

/**
 * Reuse the existing FCM deep-link path: `App.kt` already collects `AlertDeepLinkBus`
 * and routes to the Alerts tab via `PendingAlertSelectionBus` + `BottomNavSelectionBus`.
 * Entries without an `alertId` are no-ops (e.g. system / subscription notifications).
 */
private fun onEntryTap(entry: NotificationLogEntry) {
    val alertId = entry.alertId ?: return
    val payload = entry.deepLink ?: mapOf("alertId" to alertId.toString())
    AlertDeepLinkBus.emitFromData(payload)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationLogContent(
    state: NotificationLogUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (NotificationLogEntry) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones recibidas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LoadingBar(isLoading = state.isLoading)

            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = onRefresh,
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = state.isLoading,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = MaterialTheme.colorScheme.surface,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                if (state.items.isEmpty() && !state.isLoading) {
                    EmptyState(message = "Sin notificaciones recientes")
                } else {
                    val listState = rememberLazyListState()
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
                    LaunchedEffect(shouldLoadMore, state.hasMore, state.isAppending, state.isLoading) {
                        if (shouldLoadMore && state.hasMore && !state.isAppending && !state.isLoading) {
                            onLoadMore()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { entry ->
                            NotificationLogItem(entry = entry, onClick = { onItemClick(entry) })
                        }
                        if (state.isAppending) {
                            item { AppendingFooter() }
                        } else if (state.appendError != null) {
                            item { AppendErrorFooter(message = state.appendError, onRetry = onLoadMore) }
                        } else if (state.hasMore && !state.isLoading) {
                            item { LoadMoreFooter(onClick = onLoadMore) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLogItem(
    entry: NotificationLogEntry,
    onClick: () -> Unit,
) {
    val severityColor: Color = entry.severity?.let { parseHex(it.colorHex) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    val tappable = entry.alertId != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (tappable) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .background(severityColor, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title.ifBlank { entry.alertCode ?: "Notificación" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                entry.body?.takeIf { it.isNotBlank() }?.let { body ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatTimestamp(entry.sentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

// ───────── Previews ─────────

@Preview
@Composable
private fun PreviewNotificationLogContent() {
    GreenhouseTheme(darkTheme = true) {
        NotificationLogContent(
            state = NotificationLogUiState(
                isLoading = false,
                items = listOf(
                    NotificationLogEntry(
                        id = 1L,
                        alertId = 42L,
                        alertCode = "ALT-00010",
                        title = "Temperatura crítica en Sector A",
                        body = "Sensor reporta 39.8°C, supera el umbral de 35°C.",
                        severity = AlertSeverity.CRITICAL,
                        sentAt = "2026-04-30T07:14:23Z",
                        channel = "PUSH",
                        deepLink = mapOf("alertId" to "42"),
                    ),
                    NotificationLogEntry(
                        id = 2L,
                        alertId = null,
                        alertCode = null,
                        title = "Suscripción renovada",
                        body = "Tu plan ha sido renovado correctamente.",
                        severity = null,
                        sentAt = "2026-04-29T10:00:00Z",
                        channel = "PUSH",
                        deepLink = null,
                    ),
                ),
                hasMore = true,
                nextCursor = 100L,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onRefresh = {},
            onLoadMore = {},
            onItemClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewNotificationLogEmpty() {
    GreenhouseTheme(darkTheme = true) {
        NotificationLogContent(
            state = NotificationLogUiState(isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onRefresh = {},
            onLoadMore = {},
            onItemClick = {},
        )
    }
}
