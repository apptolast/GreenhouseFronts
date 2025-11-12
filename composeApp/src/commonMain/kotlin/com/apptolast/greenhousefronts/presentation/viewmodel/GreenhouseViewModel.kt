package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import com.apptolast.greenhousefronts.util.getCurrentTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GreenhouseUiState(
    val isLoading: Boolean = false,
    val recentMessages: List<GreenhouseMessage> = emptyList(),
    val lastMessage: GreenhouseMessage? = null,
    val error: String? = null,
    val publishSuccess: Boolean = false
)

class GreenhouseViewModel(
    private val repository: GreenhouseRepository = GreenhouseRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GreenhouseUiState())
    val uiState: StateFlow<GreenhouseUiState> = _uiState.asStateFlow()

    init {
        loadRecentMessages()
    }

    /**
     * Loads recent greenhouse messages
     */
    fun loadRecentMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getRecentMessages()
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recentMessages = messages,
                        lastMessage = messages.firstOrNull(),
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar mensajes: ${exception.message}"
                    )
                }
        }
    }

    /**
     * Publishes a message to the greenhouse
     *
     * @param value The setpoint value to send
     */
    fun publishSetpoint(value: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, publishSuccess = false)

            val message = GreenhouseMessage(
                timestamp = getCurrentTimestamp(),
                greenhouseId = "default",
                setpoint01 = value,
                sensor01 = null,
                sensor02 = null,
                setpoint02 = null,
                setpoint03 = null,
                rawPayload = null,
            )

            repository.publishMessage(message, topic = "GREENHOUSE/MOBILE",)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        publishSuccess = true,
                        error = null
                    )
                    // Reload messages after publishing
                    loadRecentMessages()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        publishSuccess = false,
                        error = "Error al publicar mensaje: ${exception.message}"
                    )
                }
        }
    }

    /**
     * Clears the publish success state
     */
    fun clearPublishSuccess() {
        _uiState.value = _uiState.value.copy(publishSuccess = false)
    }

    /**
     * Clears the error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}