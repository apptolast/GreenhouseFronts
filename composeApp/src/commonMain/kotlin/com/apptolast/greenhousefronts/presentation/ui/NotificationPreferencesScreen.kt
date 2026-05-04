package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.NotificationPreferences
import com.apptolast.greenhousefronts.domain.model.PreferredChannel
import com.apptolast.greenhousefronts.domain.model.QuietHours
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.NotificationPreferencesUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.NotificationPreferencesViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NotificationPreferencesScreen(
    viewModel: NotificationPreferencesViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Preferencias guardadas")
            viewModel.consumeSaveSuccess()
        }
    }
    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    NotificationPreferencesContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onCategoryAlertsChange = viewModel::setCategoryAlerts,
        onCategoryDevicesChange = viewModel::setCategoryDevices,
        onCategorySubscriptionChange = viewModel::setCategorySubscription,
        onMinSeverityChange = viewModel::setMinSeverity,
        onChannelChange = viewModel::setChannel,
        onLocaleChange = viewModel::setLocale,
        onQuietHoursEnabledChange = viewModel::setQuietHoursEnabled,
        onQuietHoursStartChange = viewModel::setQuietHoursStart,
        onQuietHoursEndChange = viewModel::setQuietHoursEnd,
        onSave = viewModel::save,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationPreferencesContent(
    state: NotificationPreferencesUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onCategoryAlertsChange: (Boolean) -> Unit,
    onCategoryDevicesChange: (Boolean) -> Unit,
    onCategorySubscriptionChange: (Boolean) -> Unit,
    onMinSeverityChange: (AlertSeverity) -> Unit,
    onChannelChange: (PreferredChannel) -> Unit,
    onLocaleChange: (String) -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    onQuietHoursStartChange: (String) -> Unit,
    onQuietHoursEndChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferencias de notificaciones") },
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
            LoadingBar(isLoading = state.isLoading || state.isSaving)

            val draft = state.draft
            if (draft == null) {
                if (!state.isLoading && state.error != null) {
                    ErrorBlock(message = state.error, onRetry = onRetry)
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CategoriesCard(
                    draft = draft,
                    onCategoryAlertsChange = onCategoryAlertsChange,
                    onCategoryDevicesChange = onCategoryDevicesChange,
                    onCategorySubscriptionChange = onCategorySubscriptionChange,
                )

                MinSeverityCard(
                    selected = draft.minSeverity,
                    onChange = onMinSeverityChange,
                )

                ChannelCard(
                    selected = draft.channel,
                    onChange = onChannelChange,
                )

                QuietHoursCard(
                    quietHours = draft.quietHours,
                    timezone = draft.timezone,
                    onEnabledChange = onQuietHoursEnabledChange,
                    onStartChange = onQuietHoursStartChange,
                    onEndChange = onQuietHoursEndChange,
                )

                LocaleCard(
                    locale = draft.locale,
                    onChange = onLocaleChange,
                )

                SaveButton(
                    canSave = state.canSave,
                    isSaving = state.isSaving,
                    onClick = onSave,
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ───────── Cards ─────────

@Composable
private fun CategoriesCard(
    draft: NotificationPreferences,
    onCategoryAlertsChange: (Boolean) -> Unit,
    onCategoryDevicesChange: (Boolean) -> Unit,
    onCategorySubscriptionChange: (Boolean) -> Unit,
) {
    SectionCard(title = "Categorías") {
        ToggleRow(
            title = "Alertas",
            subtitle = "Recibe avisos de alertas en tus invernaderos",
            checked = draft.categoryAlerts,
            onCheckedChange = onCategoryAlertsChange,
        )
        SectionDivider()
        ToggleRow(
            title = "Dispositivos",
            subtitle = "Eventos de sensores y actuadores",
            checked = draft.categoryDevices,
            onCheckedChange = onCategoryDevicesChange,
        )
        SectionDivider()
        ToggleRow(
            title = "Suscripción",
            subtitle = "Cambios en la facturación o el plan",
            checked = draft.categorySubscription,
            onCheckedChange = onCategorySubscriptionChange,
        )
    }
}

@Composable
private fun MinSeverityCard(
    selected: AlertSeverity,
    onChange: (AlertSeverity) -> Unit,
) {
    SectionCard(title = "Severidad mínima") {
        Text(
            text = "Solo recibirás notificaciones de alertas con esta severidad o superior.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            AlertSeverity.entries.forEachIndexed { index, severity ->
                SegmentedButton(
                    selected = severity == selected,
                    onClick = { onChange(severity) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AlertSeverity.entries.size,
                    ),
                ) {
                    Text(severity.display)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelCard(
    selected: PreferredChannel,
    onChange: (PreferredChannel) -> Unit,
) {
    SectionCard(title = "Canal preferido") {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = channelLabel(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text("Canal") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                PreferredChannel.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(channelLabel(option)) },
                        onClick = {
                            onChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuietHoursCard(
    quietHours: QuietHours?,
    timezone: String,
    onEnabledChange: (Boolean) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
) {
    SectionCard(title = "Horario silencioso") {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Silenciar notificaciones durante un rango horario",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = quietHours != null,
                onCheckedChange = onEnabledChange,
            )
        }
        if (quietHours != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = quietHours.start,
                    onValueChange = { input -> if (isValidHhmm(input)) onStartChange(input) },
                    label = { Text("Desde") },
                    placeholder = { Text("22:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = quietHours.end,
                    onValueChange = { input -> if (isValidHhmm(input)) onEndChange(input) },
                    label = { Text("Hasta") },
                    placeholder = { Text("07:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Zona horaria: $timezone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocaleCard(
    locale: String,
    onChange: (String) -> Unit,
) {
    SectionCard(title = "Idioma") {
        var expanded by remember { mutableStateOf(false) }
        val options = listOf(
            "es-ES" to "Español (España)",
            "en-US" to "English (US)",
        )
        val current = options.firstOrNull { it.first.equals(locale, ignoreCase = true) }
        val display = current?.second ?: locale.ifBlank { "es-ES" }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = display,
                onValueChange = {},
                readOnly = true,
                label = { Text("Idioma") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onChange(code)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// ───────── Building blocks ─────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SaveButton(
    canSave: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = canSave,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp).width(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text("Guardando…")
        } else {
            Text("Guardar")
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

private fun channelLabel(channel: PreferredChannel): String = when (channel) {
    PreferredChannel.PUSH -> "Push"
    PreferredChannel.IN_APP -> "En la app"
    PreferredChannel.EMAIL -> "Email"
}

/**
 * Permits the user to type freely up to 5 chars and matches `HH:mm` (or partial input
 * while typing). Empty string is rejected — toggling the switch off is the way to clear.
 */
private fun isValidHhmm(input: String): Boolean {
    if (input.length > 5) return false
    if (input.isEmpty()) return true
    // Allow partial input as the user types: "1", "12", "12:", "12:3", "12:30".
    val regex = Regex("^([0-9]{0,2})(:?)([0-9]{0,2})$")
    return regex.matches(input)
}

// ───────── Previews ─────────

@Preview
@Composable
private fun PreviewNotificationPreferencesContent() {
    GreenhouseTheme(darkTheme = true) {
        val sample = NotificationPreferences(
            categoryAlerts = true,
            categoryDevices = true,
            categorySubscription = false,
            minSeverity = AlertSeverity.WARNING,
            quietHours = QuietHours(start = "22:00", end = "07:00"),
            timezone = "Europe/Madrid",
            channel = PreferredChannel.PUSH,
            locale = "es-ES",
        )
        NotificationPreferencesContent(
            state = NotificationPreferencesUiState(
                isLoading = false,
                initial = sample,
                draft = sample.copy(minSeverity = AlertSeverity.ERROR),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onCategoryAlertsChange = {},
            onCategoryDevicesChange = {},
            onCategorySubscriptionChange = {},
            onMinSeverityChange = {},
            onChannelChange = {},
            onLocaleChange = {},
            onQuietHoursEnabledChange = {},
            onQuietHoursStartChange = {},
            onQuietHoursEndChange = {},
            onSave = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun PreviewNotificationPreferencesLoading() {
    GreenhouseTheme(darkTheme = true) {
        NotificationPreferencesContent(
            state = NotificationPreferencesUiState(isLoading = true),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onCategoryAlertsChange = {},
            onCategoryDevicesChange = {},
            onCategorySubscriptionChange = {},
            onMinSeverityChange = {},
            onChannelChange = {},
            onLocaleChange = {},
            onQuietHoursEnabledChange = {},
            onQuietHoursStartChange = {},
            onQuietHoursEndChange = {},
            onSave = {},
            onRetry = {},
        )
    }
}
