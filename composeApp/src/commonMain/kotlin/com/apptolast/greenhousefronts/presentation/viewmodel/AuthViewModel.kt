package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val address: String = ""
)

/**
 * One-time events for navigation.
 * Using Channel ensures events are consumed exactly once.
 */
sealed interface AuthEvent {
    data object LoginSuccess : AuthEvent
    data object RegisterSuccess : AuthEvent
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

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Channel for one-time events (navigation)
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Mutex to prevent concurrent auth operations
    private val authMutex = Mutex()

    // ========== Login Field Updates ==========

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // ========== Register Field Updates ==========

    fun updateCompanyName(value: String) {
        _uiState.update { it.copy(companyName = value) }
    }

    fun updateTaxId(value: String) {
        _uiState.update { it.copy(taxId = value) }
    }

    fun updateFirstName(value: String) {
        _uiState.update { it.copy(firstName = value) }
    }

    fun updateLastName(value: String) {
        _uiState.update { it.copy(lastName = value) }
    }

    fun updateRegisterEmail(value: String) {
        _uiState.update { it.copy(registerEmail = value) }
    }

    fun updateRegisterPassword(value: String) {
        _uiState.update { it.copy(registerPassword = value) }
    }

    fun toggleRegisterPasswordVisibility() {
        _uiState.update { it.copy(isRegisterPasswordVisible = !it.isRegisterPasswordVisible) }
    }

    fun updatePhone(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun updateAddress(value: String) {
        _uiState.update { it.copy(address = value) }
    }

    // ========== Auth Operations ==========

    /**
     * Performs login with current email and password.
     * Uses Mutex to prevent duplicate requests from rapid button clicks.
     */
    fun login() {
        // Fast path - early return if already loading
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            authMutex.withLock {
                // Double-check after acquiring mutex
                if (_uiState.value.isLoading) return@launch

                val currentState = _uiState.value

                // Basic validation
                if (currentState.email.isBlank() || currentState.password.isBlank()) {
                    _uiState.update { it.copy(error = "Por favor, completa todos los campos") }
                    return@launch
                }

                // Set loading state atomically
                _uiState.update { it.copy(isLoading = true, error = null) }

                authRepository.login(
                    email = currentState.email.trim(),
                    password = currentState.password
                )
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(AuthEvent.LoginSuccess)
                    }
                    .onFailure { error ->
                        _uiState.update {
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
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            authMutex.withLock {
                if (_uiState.value.isLoading) return@launch

                val currentState = _uiState.value

                // Basic validation
                val validationError = validateRegistration(currentState)
                if (validationError != null) {
                    _uiState.update { it.copy(error = validationError) }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, error = null) }

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
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(AuthEvent.RegisterSuccess)
                    }
                    .onFailure { error ->
                        _uiState.update {
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
     * Logs out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Reset state
            _uiState.update { AuthUiState() }
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
        _uiState.update {
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
        _uiState.update {
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
