package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.model.GreenhouseData
import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.data.model.toGroupedData
import com.apptolast.greenhousefronts.data.remote.websocket.WebSocketConnectionState
import com.apptolast.greenhousefronts.data.repository.GreenhouseRepositoryImpl
import com.apptolast.greenhousefronts.domain.repository.GreenhouseRepository
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
    val greenhouses: List<GreenhouseData> = emptyList(),
    val selectedGreenhouseId: Int = 1, // ID of the currently selected greenhouse (1, 2, or 3)
    val error: String? = null,
    val publishSuccess: Boolean = false,
    // WebSocket state
    val webSocketState: WebSocketConnectionState = WebSocketConnectionState(),
    val dataSource: DataSource = DataSource.WEBSOCKET
)

/**
 * ViewModel for greenhouse operations
 * Uses constructor injection for repository (provided by Koin)
 *
 * @param repository Injected repository for data operations
 */
class GreenhouseViewModel(
    private val repository: GreenhouseRepository
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
     * Loads recent greenhouse messages and transforms them to GreenhouseData
     */
    fun loadRecentMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getRecentMessages()
                .onSuccess { messages ->
                    val greenhouseDataList = messages.firstOrNull()?.toGroupedData() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        greenhouses = greenhouseDataList,
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
     * Updates a sector value for a specific greenhouse
     *
     * @param greenhouseId The greenhouse ID (1, 2, or 3)
     * @param sectorIndex The sector index (0-3 for sectors 1-4)
     * @param value The new value for the sector (0-100)
     */
    fun updateSector(greenhouseId: Int, sectorIndex: Int, value: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, publishSuccess = false)

            // Create message with the updated sector value
            val message = when (greenhouseId) {
                1 -> when (sectorIndex) {
                    0 -> GreenhouseMessage(invernadero01Sector01 = value)
                    1 -> GreenhouseMessage(invernadero01Sector02 = value)
                    2 -> GreenhouseMessage(invernadero01Sector03 = value)
                    3 -> GreenhouseMessage(invernadero01Sector04 = value)
                    else -> return@launch
                }

                2 -> when (sectorIndex) {
                    0 -> GreenhouseMessage(invernadero02Sector01 = value)
                    1 -> GreenhouseMessage(invernadero02Sector02 = value)
                    2 -> GreenhouseMessage(invernadero02Sector03 = value)
                    3 -> GreenhouseMessage(invernadero02Sector04 = value)
                    else -> return@launch
                }

                3 -> when (sectorIndex) {
                    0 -> GreenhouseMessage(invernadero03Sector01 = value)
                    1 -> GreenhouseMessage(invernadero03Sector02 = value)
                    2 -> GreenhouseMessage(invernadero03Sector03 = value)
                    3 -> GreenhouseMessage(invernadero03Sector04 = value)
                    else -> return@launch
                }

                else -> return@launch
            }

            repository.publishMessage(message, topic = "GREENHOUSE/MOBILE")
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
     * Updates the extractor (ventilation) value for a specific greenhouse
     *
     * @param greenhouseId The greenhouse ID (1, 2, or 3)
     * @param value The new value for the extractor (0 or 1 typically)
     */
    fun updateExtractor(greenhouseId: Int, value: Double) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(isLoading = true, error = null, publishSuccess = false)

            val message = when (greenhouseId) {
                1 -> GreenhouseMessage(invernadero01Extractor = value)
                2 -> GreenhouseMessage(invernadero02Extractor = value)
                3 -> GreenhouseMessage(invernadero03Extractor = value)
                else -> return@launch
            }

            repository.publishMessage(message, topic = "GREENHOUSE/MOBILE")
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        publishSuccess = true,
                        error = null
                    )
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
     * Selects a greenhouse to view its details
     */
    fun selectGreenhouse(greenhouseId: Int) {
        _uiState.value = _uiState.value.copy(selectedGreenhouseId = greenhouseId)
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
                    val greenhouseDataList = message.toGroupedData()
                    _uiState.value = _uiState.value.copy(
                        greenhouses = greenhouseDataList
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