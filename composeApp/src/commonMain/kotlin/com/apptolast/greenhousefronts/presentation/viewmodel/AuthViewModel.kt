package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * UI state for authentication screens (Login and Register).
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Login fields
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,

    // Register fields
    val companyName: String = "",
    val taxId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val isRegisterPasswordVisible: Boolean = false,
    val phone: String = "",
    val address: String = "",

    // Forgot Password fields
    val forgotPasswordEmail: String = "",

    // Reset Password fields
    val resetPasswordToken: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isNewPasswordVisible: Boolean = false
)

/**
 * One-time events for navigation.
 * Using Channel ensures events are consumed exactly once.
 */
sealed interface AuthEvent {
    data object LoginSuccess : AuthEvent
    data object RegisterSuccess : AuthEvent
    data object ForgotPasswordSuccess : AuthEvent
    data object ResetPasswordSuccess : AuthEvent
}

/**
 * ViewModel for authentication (login/register) screens.
 * Implements race condition prevention using Mutex and atomic state updates.
 *
 * @param authRepository Repository for auth operations
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<AuthUiState>
        field = MutableStateFlow(AuthUiState())

    // Channel for one-time events (navigation)
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Mutex to prevent concurrent auth operations
    private val authMutex = Mutex()

    // ========== Login Field Updates ==========

    fun updateEmail(email: String) {
        uiState.update { it.copy(email = email) }
    }

    fun updatePassword(password: String) {
        uiState.update { it.copy(password = password) }
    }

    fun togglePasswordVisibility() {
        uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // ========== Register Field Updates ==========

    fun updateCompanyName(value: String) {
        uiState.update { it.copy(companyName = value) }
    }

    fun updateTaxId(value: String) {
        uiState.update { it.copy(taxId = value) }
    }

    fun updateFirstName(value: String) {
        uiState.update { it.copy(firstName = value) }
    }

    fun updateLastName(value: String) {
        uiState.update { it.copy(lastName = value) }
    }

    fun updateRegisterEmail(value: String) {
        uiState.update { it.copy(registerEmail = value) }
    }

    fun updateRegisterPassword(value: String) {
        uiState.update { it.copy(registerPassword = value) }
    }

    fun toggleRegisterPasswordVisibility() {
        uiState.update { it.copy(isRegisterPasswordVisible = !it.isRegisterPasswordVisible) }
    }

    fun updatePhone(value: String) {
        uiState.update { it.copy(phone = value) }
    }

    fun updateAddress(value: String) {
        uiState.update { it.copy(address = value) }
    }

    fun updateForgotPasswordEmail(value: String) {
        uiState.update { it.copy(forgotPasswordEmail = value) }
    }

    fun updateResetPasswordToken(value: String) {
        uiState.update { it.copy(resetPasswordToken = value) }
    }

    fun updateNewPassword(value: String) {
        uiState.update { it.copy(newPassword = value) }
    }

    fun updateConfirmNewPassword(value: String) {
        uiState.update { it.copy(confirmNewPassword = value) }
    }

    fun toggleNewPasswordVisibility() {
        uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    // ========== Auth Operations ==========

    /**
     * Performs login with current email and password.
     * Uses Mutex to prevent duplicate requests from rapid button clicks.
     */
    fun login() {
        // Fast path - early return if already loading
        if (uiState.value.isLoading) return

        viewModelScope.launch {
            authMutex.withLock {
                // Double-check after acquiring mutex
                if (uiState.value.isLoading) return@launch

                val currentState = uiState.value

                // Basic validation
                if (currentState.email.isBlank() || currentState.password.isBlank()) {
                    uiState.update { it.copy(error = "Por favor, completa todos los campos") }
                    return@launch
                }

                // Set loading state atomically
                uiState.update { it.copy(isLoading = true, error = null) }

                authRepository.login(
                    email = currentState.email.trim(),
                    password = currentState.password
                )
                    .onSuccess {
                        uiState.update { it.copy(isLoading = false) }
                        _events.send(AuthEvent.LoginSuccess)
                    }
                    .onFailure { error ->
                        uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error al iniciar sesión"
                            )
                        }
                    }
            }
        }
    }

    /**
     * Performs registration with current form data.
     * Uses Mutex to prevent duplicate requests.
     */
    fun register() {
        // Fast path
        if (uiState.value.isLoading) return

        viewModelScope.launch {
            authMutex.withLock {
                if (uiState.value.isLoading) return@launch

                val currentState = uiState.value

                // Basic validation
                val validationError = validateRegistration(currentState)
                if (validationError != null) {
                    uiState.update { it.copy(error = validationError) }
                    return@launch
                }

                uiState.update { it.copy(isLoading = true, error = null) }

                val request = RegisterRequest(
                    companyName = currentState.companyName.trim(),
                    taxId = currentState.taxId.trim(),
                    email = currentState.registerEmail.trim(),
                    password = currentState.registerPassword,
                    firstName = currentState.firstName.trim(),
                    lastName = currentState.lastName.trim(),
                    phone = currentState.phone.trim().ifBlank { null },
                    address = currentState.address.trim().ifBlank { null }
                )

                authRepository.register(request)
                    .onSuccess {
                        uiState.update { it.copy(isLoading = false) }
                        _events.send(AuthEvent.RegisterSuccess)
                    }
                    .onFailure { error ->
                        uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error en el registro"
                            )
                        }
                    }
            }
        }
    }

    /**
     * Requests a password reset email.
     */
    fun forgotPassword() {
        if (uiState.value.isLoading) return

        viewModelScope.launch {
            authMutex.withLock {
                if (uiState.value.isLoading) return@launch

                val currentState = uiState.value

                if (currentState.forgotPasswordEmail.isBlank()) {
                    uiState.update { it.copy(error = "Por favor, ingresa tu email") }
                    return@launch
                }

                if (!isValidEmail(currentState.forgotPasswordEmail)) {
                    uiState.update { it.copy(error = "El email no es válido") }
                    return@launch
                }

                uiState.update { it.copy(isLoading = true, error = null) }

                authRepository.forgotPassword(currentState.forgotPasswordEmail.trim())
                    .onSuccess {
                        uiState.update { it.copy(isLoading = false) }
                        _events.send(AuthEvent.ForgotPasswordSuccess)
                    }
                    .onFailure { error ->
                        uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error al solicitar recuperación"
                            )
                        }
                    }
            }
        }
    }

    /**
     * Resets the password using the token and new password.
     */
    fun resetPassword() {
        if (uiState.value.isLoading) return

        viewModelScope.launch {
            authMutex.withLock {
                if (uiState.value.isLoading) return@launch

                val currentState = uiState.value

                if (currentState.newPassword.isBlank()) {
                    uiState.update { it.copy(error = "Por favor, ingresa la nueva contraseña") }
                    return@launch
                }

                if (currentState.newPassword.length < 6) {
                    uiState.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
                    return@launch
                }

                if (currentState.newPassword != currentState.confirmNewPassword) {
                    uiState.update { it.copy(error = "Las contraseñas no coinciden") }
                    return@launch
                }

                uiState.update { it.copy(isLoading = true, error = null) }

                authRepository.resetPassword(
                    currentState.resetPasswordToken,
                    currentState.newPassword
                )
                    .onSuccess {
                        uiState.update { it.copy(isLoading = false) }
                        _events.send(AuthEvent.ResetPasswordSuccess)
                    }
                    .onFailure { error ->
                        uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error al restablecer contraseña"
                            )
                        }
                    }
            }
        }
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Reset state
            uiState.update { AuthUiState() }
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    /**
     * Checks if user is currently logged in.
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    /**
     * Clears login form fields.
     */
    fun clearLoginForm() {
        uiState.update {
            it.copy(
                email = "",
                password = "",
                isPasswordVisible = false
            )
        }
    }

    /**
     * Clears register form fields.
     */
    fun clearRegisterForm() {
        uiState.update {
            it.copy(
                companyName = "",
                taxId = "",
                firstName = "",
                lastName = "",
                registerEmail = "",
                registerPassword = "",
                isRegisterPasswordVisible = false,
                phone = "",
                address = ""
            )
        }
    }

    fun clearForgotPasswordForm() {
        uiState.update {
            it.copy(forgotPasswordEmail = "")
        }
    }

    fun clearResetPasswordForm() {
        uiState.update {
            it.copy(
                resetPasswordToken = "",
                newPassword = "",
                confirmNewPassword = "",
                isNewPasswordVisible = false
            )
        }
    }

    /**
     * Validates registration form fields.
     * @return Error message if validation fails, null if valid
     */
    private fun validateRegistration(state: AuthUiState): String? {
        return when {
            state.companyName.isBlank() -> "El nombre de empresa es obligatorio"
            state.companyName.length < 2 -> "El nombre de empresa debe tener al menos 2 caracteres"
            state.companyName.length > 100 -> "El nombre de empresa no puede superar 100 caracteres"
            state.taxId.isBlank() -> "El NIF/CIF es obligatorio"
            state.registerEmail.isBlank() -> "El email es obligatorio"
            !isValidEmail(state.registerEmail) -> "El email no es válido"
            state.registerPassword.isBlank() -> "La contraseña es obligatoria"
            state.registerPassword.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            state.firstName.isBlank() -> "El nombre es obligatorio"
            state.lastName.isBlank() -> "El apellido es obligatorio"
            else -> null
        }
    }

    /**
     * Basic email validation.
     */
    private fun isValidEmail(email: String): Boolean {
        // Use a robust regex for email validation
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
}
