package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.domain.model.Greenhouse
import com.apptolast.greenhousefronts.presentation.ui.components.BottomNavBar
import com.apptolast.greenhousefronts.presentation.ui.components.BottomNavTab
import com.apptolast.greenhousefronts.presentation.ui.components.GreenhouseCard
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseListUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseListViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.ProfileViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main screen after authentication.
 * Contains a Scaffold with bottom navigation and tab content switching.
 */
@Composable
fun MainScreen(
    greenhouseListViewModel: GreenhouseListViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    onLogoutSuccess: () -> Unit = {},
    onNavigateToGreenhouseDetail: (Long) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(BottomNavTab.GREENHOUSES) }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (selectedTab) {
                BottomNavTab.GREENHOUSES -> {
                    LifecycleResumeEffect(Unit) {
                        greenhouseListViewModel.loadGreenhouses()
                        onPauseOrDispose { }
                    }
                    val uiState by greenhouseListViewModel.uiState.collectAsState()
                    GreenhouseListContent(
                        uiState = uiState,
                        onRetry = greenhouseListViewModel::loadGreenhouses,
                        onGreenhouseClick = { onNavigateToGreenhouseDetail(it.id) },
                    )
                }

                BottomNavTab.ALERTS -> PlaceholderTab("Alertas")

                BottomNavTab.PROFILE -> {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onLogoutSuccess = onLogoutSuccess,
                    )
                }
            }
        }
    }
}

@Composable
private fun GreenhouseListContent(
    uiState: GreenhouseListUiState,
    onRetry: () -> Unit,
    onGreenhouseClick: (Greenhouse) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                text = "🌱 GreenhouseFronts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (uiState.displayName.isNotBlank()) {
                Text(
                    text = "Hola, ${uiState.displayName} 🌿",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Section title
                item {
                    Text(
                        text = "Mis Invernaderos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                // Greenhouse cards
                if (uiState.greenhouses.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No hay invernaderos registrados",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    val rows = uiState.greenhouses.chunked(2)
                    items(
                        count = rows.size,
                        key = { rows[it].first().id },
                    ) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rows[rowIndex].forEach { greenhouse ->
                                GreenhouseCard(
                                    greenhouse = greenhouse,
                                    onClick = { onGreenhouseClick(greenhouse) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rows[rowIndex].size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = onRetry) {
                            Text("Reintentar", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTab(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun PreviewGreenhouseListContent() {
    GreenhouseTheme(darkTheme = true) {
        Scaffold(
            bottomBar = {
                BottomNavBar(
                    selectedTab = BottomNavTab.GREENHOUSES,
                    onTabSelected = {},
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(Modifier.padding(padding)) {
                GreenhouseListContent(
                    uiState = GreenhouseListUiState(
                        isLoading = false,
                        displayName = "Carlos",
                        greenhouses = listOf(
                            Greenhouse(1L, "GRH-00001", "Invernadero Norte", true, 2500.0, 4, 2),
                            Greenhouse(2L, "GRH-00002", "Invernadero Sur", true, 1800.0, 3, 1),
                            Greenhouse(3L, "GRH-00003", "Invernadero Este", false, 3200.0, 5, 0),
                        ),
                    ),
                    onRetry = {},
                    onGreenhouseClick = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewGreenhouseListLoading() {
    GreenhouseTheme(darkTheme = true) {
        GreenhouseListContent(
            uiState = GreenhouseListUiState(isLoading = true),
            onRetry = {},
            onGreenhouseClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewGreenhouseListEmpty() {
    GreenhouseTheme(darkTheme = true) {
        GreenhouseListContent(
            uiState = GreenhouseListUiState(isLoading = false, displayName = "Carlos"),
            onRetry = {},
            onGreenhouseClick = {},
        )
    }
}
