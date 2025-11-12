package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.remote.websocket.WebSocketConnectionState
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
import com.apptolast.greenhousefronts.util.getCurrentTimestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Data source indicator for greenhouse messages
 */
enum class DataSource {
    HTTP,      // Using HTTP polling
    WEBSOCKET  // Using WebSocket real-time
}

data class GreenhouseUiState(
    val isLoading: Boolean = false,
    val recentMessages: List<GreenhouseMessage> = emptyList(),
    val lastMessage: GreenhouseMessage? = null,
    val error: String? = null,
    val publishSuccess: Boolean = false,
    // WebSocket state
    val webSocketState: WebSocketConnectionState = WebSocketConnectionState(),
    val realtimeMessage: GreenhouseMessage? = null,
    val dataSource: DataSource = DataSource.WEBSOCKET
)

class GreenhouseViewModel(
    private val repository: GreenhouseRepository = GreenhouseRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GreenhouseUiState())
    val uiState: StateFlow<GreenhouseUiState> = _uiState.asStateFlow()

    private var webSocketJob: Job? = null
    private var connectionStateJob: Job? = null
    private val maxReconnectAttempts = 5

    init {
        loadRecentMessages()
        connectToWebSocket()
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

    // WebSocket methods

    /**
     * Connect to WebSocket and start observing real-time messages
     * Implements automatic retry with exponential backoff
     */
    private fun connectToWebSocket() {
        viewModelScope.launch {
            val currentAttempts = _uiState.value.webSocketState.reconnectAttempts

            if (currentAttempts >= maxReconnectAttempts) {
                // Max attempts reached, fallback to HTTP
                println("Max WebSocket reconnect attempts reached, using HTTP fallback")
                _uiState.value = _uiState.value.copy(
                    dataSource = DataSource.HTTP,
                    webSocketState = _uiState.value.webSocketState.copy(
                        lastError = "Failed to connect after $maxReconnectAttempts attempts. Using HTTP fallback."
                    )
                )
                return@launch
            }

            repository.connectWebSocket()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        dataSource = DataSource.WEBSOCKET
                    )
                    observeRealtimeMessages()
                    observeConnectionState()
                }
                .onFailure { exception ->
                    println("WebSocket connection failed: ${exception.message}")
                    // Exponential backoff: 1s, 2s, 4s, 8s, 16s
                    val delayMs = (1000L * (1 shl currentAttempts)).coerceAtMost(16000L)
                    delay(delayMs)
                    connectToWebSocket() // Retry
                }
        }
    }

    /**
     * Observe real-time messages from WebSocket
     */
    private fun observeRealtimeMessages() {
        webSocketJob?.cancel()
        webSocketJob = viewModelScope.launch {
            repository.observeRealtimeMessages()
                .catch { e ->
                    println("Error observing messages: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        dataSource = DataSource.HTTP,
                        error = "WebSocket error: ${e.message}. Switching to HTTP."
                    )
                    // Connection lost, try to reconnect
                    delay(2000)
                    connectToWebSocket()
                }
                .collect { message ->
                    _uiState.value = _uiState.value.copy(
                        realtimeMessage = message,
                        lastMessage = message,
                        recentMessages = listOf(message) + _uiState.value.recentMessages.take(99)
                    )
                }
        }
    }

    /**
     * Observe WebSocket connection state changes
     */
    private fun observeConnectionState() {
        connectionStateJob?.cancel()
        connectionStateJob = viewModelScope.launch {
            repository.getConnectionState().collect { state ->
                _uiState.value = _uiState.value.copy(
                    webSocketState = state
                )

                // If disconnected unexpectedly, try to reconnect
                if (!state.isConnected && _uiState.value.dataSource == DataSource.WEBSOCKET) {
                    delay(3000)
                    connectToWebSocket()
                }
            }
        }
    }

    /**
     * Manually reconnect to WebSocket
     * Used by UI reconnect button
     */
    fun reconnectWebSocket() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                webSocketState = _uiState.value.webSocketState.copy(
                    reconnectAttempts = 0 // Reset attempts for manual reconnection
                )
            )
            repository.reconnectWebSocket()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        dataSource = DataSource.WEBSOCKET
                    )
                    observeRealtimeMessages()
                    observeConnectionState()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to reconnect: ${exception.message}"
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketJob?.cancel()
        connectionStateJob?.cancel()
        viewModelScope.launch {
            repository.disconnectWebSocket()
        }
    }
}