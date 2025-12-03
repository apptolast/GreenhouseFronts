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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthEvent
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthUiState
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
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Registration screen (Stateful).
 * It observes the ViewModel's state and handles registration events.
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.RegisterSuccess && !hasNavigated) {
                hasNavigated = true
                onRegisterSuccess()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    RegisterScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onFieldChange = {
            when (it) {
                is RegisterField.CompanyName -> viewModel.updateCompanyName(it.value)
                is RegisterField.TaxId -> viewModel.updateTaxId(it.value)
                is RegisterField.FirstName -> viewModel.updateFirstName(it.value)
                is RegisterField.LastName -> viewModel.updateLastName(it.value)
                is RegisterField.Email -> viewModel.updateRegisterEmail(it.value)
                is RegisterField.Password -> viewModel.updateRegisterPassword(it.value)
                is RegisterField.Phone -> viewModel.updatePhone(it.value)
                is RegisterField.Address -> viewModel.updateAddress(it.value)
            }
        },
        onRegisterClick = viewModel::register,
        onTogglePasswordVisibility = viewModel::toggleRegisterPasswordVisibility,
        onNavigateToLogin = onNavigateToLogin
    )
}

/**
 * Content for the registration screen (Stateless).
 * Displays the UI and delegates user actions to the callers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreenContent(
    uiState: AuthUiState,
    snackbarHostState: SnackbarHostState,
    onFieldChange: (RegisterField) -> Unit,
    onRegisterClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
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
                // Header
                Text(
                    stringResource(Res.string.register_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Form Fields
                RegisterTextField(
                    uiState.companyName,
                    { onFieldChange(RegisterField.CompanyName(it)) },
                    stringResource(Res.string.register_company_name_label),
                    stringResource(Res.string.register_company_name_placeholder),
                    Icons.Default.Business,
                    stringResource(Res.string.cd_company_icon),
                    !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                RegisterTextField(
                    uiState.taxId,
                    { onFieldChange(RegisterField.TaxId(it)) },
                    stringResource(Res.string.register_tax_id_label),
                    stringResource(Res.string.register_tax_id_placeholder),
                    Icons.Default.Receipt,
                    stringResource(Res.string.cd_tax_id_icon),
                    !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RegisterTextField(
                        uiState.firstName,
                        { onFieldChange(RegisterField.FirstName(it)) },
                        stringResource(Res.string.register_first_name_label),
                        stringResource(Res.string.register_first_name_placeholder),
                        Icons.Default.Person,
                        stringResource(Res.string.cd_name_icon),
                        !uiState.isLoading,
                        Modifier.weight(1f)
                    )
                    RegisterTextField(
                        uiState.lastName,
                        { onFieldChange(RegisterField.LastName(it)) },
                        stringResource(Res.string.register_last_name_label),
                        stringResource(Res.string.register_last_name_placeholder),
                        Icons.Default.Person,
                        stringResource(Res.string.cd_name_icon),
                        !uiState.isLoading,
                        Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                RegisterTextField(
                    uiState.registerEmail,
                    { onFieldChange(RegisterField.Email(it)) },
                    stringResource(Res.string.register_email_label),
                    stringResource(Res.string.register_email_placeholder),
                    Icons.Default.Email,
                    stringResource(Res.string.cd_email_icon),
                    !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.registerPassword,
                    onValueChange = { onFieldChange(RegisterField.Password(it)) },
                    label = { Text(stringResource(Res.string.register_password_label)) },
                    placeholder = {
                        Text(
                            stringResource(Res.string.register_password_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(Res.string.cd_password_icon),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isRegisterPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (uiState.isRegisterPasswordVisible) stringResource(
                                    Res.string.cd_password_hide
                                ) else stringResource(Res.string.cd_password_show),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    visualTransformation = if (uiState.isRegisterPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                RegisterTextField(
                    uiState.phone,
                    { onFieldChange(RegisterField.Phone(it)) },
                    stringResource(Res.string.register_phone_label),
                    stringResource(Res.string.register_phone_placeholder),
                    Icons.Default.Phone,
                    stringResource(Res.string.cd_phone_icon),
                    !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                RegisterTextField(
                    uiState.address,
                    { onFieldChange(RegisterField.Address(it)) },
                    stringResource(Res.string.register_address_label),
                    stringResource(Res.string.register_address_placeholder),
                    Icons.Default.Home,
                    stringResource(Res.string.cd_back),
                    !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isLoading && uiState.companyName.isNotBlank() && uiState.taxId.isNotBlank() && uiState.firstName.isNotBlank() && uiState.lastName.isNotBlank() && uiState.registerEmail.isNotBlank() && uiState.registerPassword.isNotBlank(),
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
                            stringResource(Res.string.register_button),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.register_login_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onNavigateToLogin, enabled = !uiState.isLoading) {
                        Text(
                            stringResource(Res.string.register_login_link),
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

@Preview
@Composable
private fun RegisterScreenContentPreview() {
    val uiState = AuthUiState(
        companyName = "Greenhouse Inc.",
        taxId = "B12345678",
        firstName = "John",
        lastName = "Doe",
        registerEmail = "john.doe@example.com",
        registerPassword = "password123",
        phone = "600123456",
        address = "123 Main St, Anytown"
    )

    GreenhouseTheme {
        RegisterScreenContent(uiState, remember { SnackbarHostState() }, {}, {}, {}, {})
    }
}

/**
 * Reusable text field component for the registration form.
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
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                leadingIcon,
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

@Preview
@Composable
private fun RegisterTextFieldPreview() {
    GreenhouseTheme {
        RegisterTextField(
            value = "Sample Text",
            onValueChange = {},
            label = "Sample Label",
            placeholder = "Sample Placeholder",
            leadingIcon = Icons.Default.Business,
            contentDescription = "Sample Icon",
            enabled = true
        )
    }
}

/**
 * Sealed interface to represent the different text fields in the registration form.
 * Used to avoid creating multiple lambdas for each field.
 */
sealed class RegisterField {
    data class CompanyName(val value: String) : RegisterField()
    data class TaxId(val value: String) : RegisterField()
    data class FirstName(val value: String) : RegisterField()
    data class LastName(val value: String) : RegisterField()
    data class Email(val value: String) : RegisterField()
    data class Password(val value: String) : RegisterField()
    data class Phone(val value: String) : RegisterField()
    data class Address(val value: String) : RegisterField()
}
