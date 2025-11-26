package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthEvent
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.cd_back
import greenhousefronts.composeapp.generated.resources.cd_company_icon
import greenhousefronts.composeapp.generated.resources.cd_email_icon
import greenhousefronts.composeapp.generated.resources.cd_name_icon
import greenhousefronts.composeapp.generated.resources.cd_password_hide
import greenhousefronts.composeapp.generated.resources.cd_password_icon
import greenhousefronts.composeapp.generated.resources.cd_password_show
import greenhousefronts.composeapp.generated.resources.cd_phone_icon
import greenhousefronts.composeapp.generated.resources.cd_tax_id_icon
import greenhousefronts.composeapp.generated.resources.register_address_label
import greenhousefronts.composeapp.generated.resources.register_address_placeholder
import greenhousefronts.composeapp.generated.resources.register_button
import greenhousefronts.composeapp.generated.resources.register_company_name_label
import greenhousefronts.composeapp.generated.resources.register_company_name_placeholder
import greenhousefronts.composeapp.generated.resources.register_email_label
import greenhousefronts.composeapp.generated.resources.register_email_placeholder
import greenhousefronts.composeapp.generated.resources.register_first_name_label
import greenhousefronts.composeapp.generated.resources.register_first_name_placeholder
import greenhousefronts.composeapp.generated.resources.register_last_name_label
import greenhousefronts.composeapp.generated.resources.register_last_name_placeholder
import greenhousefronts.composeapp.generated.resources.register_login_link
import greenhousefronts.composeapp.generated.resources.register_login_prompt
import greenhousefronts.composeapp.generated.resources.register_password_label
import greenhousefronts.composeapp.generated.resources.register_password_placeholder
import greenhousefronts.composeapp.generated.resources.register_phone_label
import greenhousefronts.composeapp.generated.resources.register_phone_placeholder
import greenhousefronts.composeapp.generated.resources.register_subtitle
import greenhousefronts.composeapp.generated.resources.register_tax_id_label
import greenhousefronts.composeapp.generated.resources.register_tax_id_placeholder
import greenhousefronts.composeapp.generated.resources.register_title
import org.jetbrains.compose.resources.stringResource

/**
 * Registration screen for new tenant/company registration.
 * Follows the same visual style as LoginScreen.
 *
 * @param viewModel AuthViewModel for state management
 * @param onRegisterSuccess Callback when registration succeeds
 * @param onNavigateToLogin Callback to navigate back to login
 */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasNavigated by remember { mutableStateOf(false) }

    // Handle one-time navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.RegisterSuccess -> {
                    if (!hasNavigated) {
                        hasNavigated = true
                        onRegisterSuccess()
                    }
                }

                else -> { /* Login events handled in LoginScreen */
                }
            }
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = stringResource(Res.string.register_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = stringResource(Res.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Company Name
                RegisterTextField(
                    value = uiState.companyName,
                    onValueChange = viewModel::updateCompanyName,
                    label = stringResource(Res.string.register_company_name_label),
                    placeholder = stringResource(Res.string.register_company_name_placeholder),
                    leadingIcon = Icons.Default.Business,
                    contentDescription = stringResource(Res.string.cd_company_icon),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tax ID
                RegisterTextField(
                    value = uiState.taxId,
                    onValueChange = viewModel::updateTaxId,
                    label = stringResource(Res.string.register_tax_id_label),
                    placeholder = stringResource(Res.string.register_tax_id_placeholder),
                    leadingIcon = Icons.Default.Receipt,
                    contentDescription = stringResource(Res.string.cd_tax_id_icon),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // First Name & Last Name in row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RegisterTextField(
                        value = uiState.firstName,
                        onValueChange = viewModel::updateFirstName,
                        label = stringResource(Res.string.register_first_name_label),
                        placeholder = stringResource(Res.string.register_first_name_placeholder),
                        leadingIcon = Icons.Default.Person,
                        contentDescription = stringResource(Res.string.cd_name_icon),
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    )

                    RegisterTextField(
                        value = uiState.lastName,
                        onValueChange = viewModel::updateLastName,
                        label = stringResource(Res.string.register_last_name_label),
                        placeholder = stringResource(Res.string.register_last_name_placeholder),
                        leadingIcon = Icons.Default.Person,
                        contentDescription = stringResource(Res.string.cd_name_icon),
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Email
                RegisterTextField(
                    value = uiState.registerEmail,
                    onValueChange = viewModel::updateRegisterEmail,
                    label = stringResource(Res.string.register_email_label),
                    placeholder = stringResource(Res.string.register_email_placeholder),
                    leadingIcon = Icons.Default.Email,
                    contentDescription = stringResource(Res.string.cd_email_icon),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = uiState.registerPassword,
                    onValueChange = viewModel::updateRegisterPassword,
                    label = { Text(stringResource(Res.string.register_password_label)) },
                    placeholder = {
                        Text(
                            stringResource(Res.string.register_password_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(Res.string.cd_password_icon),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = viewModel::toggleRegisterPasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isRegisterPasswordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (uiState.isRegisterPasswordVisible) {
                                    stringResource(Res.string.cd_password_hide)
                                } else {
                                    stringResource(Res.string.cd_password_show)
                                },
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    visualTransformation = if (uiState.isRegisterPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone (optional)
                RegisterTextField(
                    value = uiState.phone,
                    onValueChange = viewModel::updatePhone,
                    label = stringResource(Res.string.register_phone_label),
                    placeholder = stringResource(Res.string.register_phone_placeholder),
                    leadingIcon = Icons.Default.Phone,
                    contentDescription = stringResource(Res.string.cd_phone_icon),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Address (optional)
                RegisterTextField(
                    value = uiState.address,
                    onValueChange = viewModel::updateAddress,
                    label = stringResource(Res.string.register_address_label),
                    placeholder = stringResource(Res.string.register_address_placeholder),
                    leadingIcon = Icons.Default.Home,
                    contentDescription = stringResource(Res.string.cd_back),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Register button
                Button(
                    onClick = viewModel::register,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isLoading &&
                            uiState.companyName.isNotBlank() &&
                            uiState.taxId.isNotBlank() &&
                            uiState.firstName.isNotBlank() &&
                            uiState.lastName.isNotBlank() &&
                            uiState.registerEmail.isNotBlank() &&
                            uiState.registerPassword.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.register_button),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.register_login_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            text = stringResource(Res.string.register_login_link),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Reusable text field component for registration form.
 */
@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}
