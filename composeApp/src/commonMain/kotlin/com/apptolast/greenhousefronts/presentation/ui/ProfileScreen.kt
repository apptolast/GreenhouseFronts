package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.UserProfile
import com.apptolast.greenhousefronts.getPlatform
import com.apptolast.greenhousefronts.presentation.ui.components.LoadingBar
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.ProfileEvent
import com.apptolast.greenhousefronts.presentation.viewmodel.ProfileUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.ProfileViewModel
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.app_name
import greenhousefronts.composeapp.generated.resources.profile_notif_alerts_desc
import greenhousefronts.composeapp.generated.resources.profile_notif_alerts_title
import greenhousefronts.composeapp.generated.resources.profile_notif_min_severity_desc
import greenhousefronts.composeapp.generated.resources.profile_notif_min_severity_title
import greenhousefronts.composeapp.generated.resources.profile_section_notifications
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.LogoutSuccess -> onLogoutSuccess()
            }
        }
    }

    ProfileContent(
        uiState = uiState,
        onLogout = viewModel::logout,
        onRetry = viewModel::loadProfile,
        onAlertsEnabledChange = viewModel::setAlertsEnabled,
        onMinSeverityChange = viewModel::setMinSeverity,
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onRetry: () -> Unit,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onMinSeverityChange: (AlertSeverity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LoadingBar(isLoading = uiState.isLoading)

        if (uiState.error != null && uiState.profile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }

        uiState.profile?.let { profile ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // User info card
                ProfileSectionCard(title = "Cuenta") {
                    ProfileInfoRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = profile.email,
                    )

                    ProfileDivider()

                    ProfileInfoRow(
                        icon = Icons.Default.Shield,
                        label = "Rol",
                        value = formatRole(profile.role),
                    )

                    profile.code?.let { code ->
                        ProfileDivider()
                        ProfileInfoRow(
                            icon = Icons.Default.Badge,
                            label = "Código",
                            value = code,
                        )
                    }

                    profile.lastLogin?.let { lastLogin ->
                        ProfileDivider()
                        ProfileInfoRow(
                            icon = Icons.AutoMirrored.Filled.Login,
                            label = "Último acceso",
                            value = formatTimestamp(lastLogin),
                        )
                    }

                    profile.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
                        ProfileDivider()
                        ProfileInfoRow(
                            icon = Icons.Default.CalendarMonth,
                            label = "Miembro desde",
                            value = formatTimestamp(createdAt),
                        )
                    }
                }

                // Company info card (only if tenant data is available)
                if (profile.companyName != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileSectionCard(title = "Empresa") {
                        ProfileInfoRow(
                            icon = Icons.Default.Business,
                            label = "Nombre",
                            value = profile.companyName,
                        )

                        profile.companyPhone?.let { phone ->
                            ProfileDivider()
                            ProfileInfoRow(
                                icon = Icons.Default.Phone,
                                label = "Teléfono",
                                value = phone,
                            )
                        }

                        val location = buildLocationString(profile.province, profile.country)
                        if (location != null) {
                            ProfileDivider()
                            ProfileInfoRow(
                                icon = Icons.Default.LocationOn,
                                label = "Ubicación",
                                value = location,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inline notification preferences: alerts toggle + min severity selector.
                ProfileSectionCard(title = stringResource(Res.string.profile_section_notifications)) {
                    // Alerts enabled toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.profile_notif_alerts_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.profile_notif_alerts_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.alertsEnabled,
                            onCheckedChange = onAlertsEnabledChange,
                        )
                    }

                    ProfileDivider()

                    // Minimum severity selector
                    Column {
                        Text(
                            text = stringResource(Res.string.profile_notif_min_severity_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.profile_notif_min_severity_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            AlertSeverity.entries.forEachIndexed { index, severity ->
                                SegmentedButton(
                                    selected = severity == uiState.minSeverity,
                                    onClick = { onMinSeverityChange(severity) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AlertSeverity.entries.size,
                                    ),
                                    enabled = uiState.alertsEnabled,
                                ) {
                                    Text(severity.display)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Logout button
                val logoutRed = Color(0xFFFF1744)
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isLoggingOut,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, logoutRed),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = logoutRed.copy(alpha = 0.12f),
                        contentColor = logoutRed,
                    ),
                ) {
                    if (uiState.isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = logoutRed,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isLoggingOut) "Cerrando sesión..." else "Cerrar sesión",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // App version
                Text(
                    text = "${stringResource(Res.string.app_name)} v${getPlatform().versionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val (backgroundColor, textColor) = when (role.uppercase()) {
        "ADMIN" -> Color(0xFF1B3A1B) to Color(0xFF4CAF50)
        "OPERATOR" -> Color(0xFF1B2A3A) to Color(0xFF42A5F5)
        else -> Color(0xFF2A2A2A) to Color(0xFFBDBDBD)
    }

    Box(
        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(20.dp)).background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = formatRole(role),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ProfileSectionCard(
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
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

private fun buildLocationString(province: String?, country: String?): String? {
    val parts = listOfNotNull(province, country)
    return parts.joinToString(", ").ifBlank { null }
}

private fun formatRole(role: String): String {
    return when (role.uppercase()) {
        "ADMIN" -> "Administrador"
        "OPERATOR" -> "Operador"
        "VIEWER" -> "Visualizador"
        else -> role
    }
}

private fun formatTimestamp(iso: String): String {
    return iso.substringBefore("T").ifBlank { iso }
}

@Preview
@Composable
private fun PreviewProfileContent() {
    GreenhouseTheme(darkTheme = true) {
        ProfileContent(
            uiState = ProfileUiState(
                isLoading = false,
                alertsEnabled = true,
                minSeverity = AlertSeverity.INFO,
                profile = UserProfile(
                    id = 1L,
                    code = "USR-00001",
                    username = "Carlos García",
                    email = "carlos@invernaderos.com",
                    role = "ADMIN",
                    isActive = true,
                    lastLogin = "2026-03-16T10:30:00Z",
                    createdAt = "2026-01-15T08:00:00Z",
                    companyName = "Invernaderos García S.L.",
                    companyPhone = "+34 950 123 456",
                    province = "Almería",
                    country = "España",
                ),
            ),
            onLogout = {},
            onRetry = {},
            onAlertsEnabledChange = {},
            onMinSeverityChange = {},
        )
    }
}

@Preview
@Composable
private fun PreviewProfileContentAlertsDisabled() {
    GreenhouseTheme(darkTheme = true) {
        ProfileContent(
            uiState = ProfileUiState(
                isLoading = false,
                alertsEnabled = false,
                minSeverity = AlertSeverity.WARNING,
                profile = UserProfile(
                    id = 2L,
                    code = null,
                    username = "Ana López",
                    email = "ana@invernaderos.com",
                    role = "OPERATOR",
                    isActive = true,
                    lastLogin = null,
                    createdAt = null,
                    companyName = null,
                    companyPhone = null,
                    province = null,
                    country = null,
                ),
            ),
            onLogout = {},
            onRetry = {},
            onAlertsEnabledChange = {},
            onMinSeverityChange = {},
        )
    }
}

@Preview
@Composable
private fun PreviewProfileLoading() {
    GreenhouseTheme(darkTheme = true) {
        ProfileContent(
            uiState = ProfileUiState(isLoading = true),
            onLogout = {},
            onRetry = {},
            onAlertsEnabledChange = {},
            onMinSeverityChange = {},
        )
    }
}
