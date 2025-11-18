package com.apptolast.greenhousefronts.data.remote.websocket

import com.apptolast.greenhousefronts.data.model.GreenhouseMessage
import com.apptolast.greenhousefronts.util.Environment
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.conversions.kxserialization.json.withJsonConversions
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * WebSocket client for STOMP protocol connection to greenhouse data stream
 * Handles connection lifecycle, message subscription, and connection state tracking
 * Uses constructor injection for StompClient (provided by Koin)
 *
 * @param stompClient Injected STOMP client for WebSocket communication
 */
class StompWebSocketClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {

    private var session: StompSession? = null

    private val _connectionState = MutableStateFlow(WebSocketConnectionState())
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    /**
     * WebSocket URL derived from current environment
     * Converts HTTP(S) URL to WS(S) and appends WebSocket endpoint
     */
    private val webSocketUrl: String
        get() = Environment.current.baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws/greenhouse-native"

    /**
     * Connect to the WebSocket server
     *
     * @return Result indicating success or failure with error details
     */
    @OptIn(ExperimentalTime::class)
    suspend fun connect(): Result<Unit> {
        return try {
            val connectionTime = Clock.System.now().toEpochMilliseconds()

            val wsClient = KtorWebSocketClient(httpClient)

            val stompConfig = StompConfig().apply {
                // Connection timeout
                connectionTimeout = 10.seconds

                // Timeout para recibir RECEIPT frames
                receiptTimeout = 5.seconds

                // Timeout para disconnect graceful
                disconnectTimeout = 3.seconds

                // Auto receipt - espera confirmación del servidor
                autoReceipt = false // Deshabilitado para mejor rendimiento

                // Graceful disconnect - cierre limpio
                gracefulDisconnect = true

                // Conectar con protocolo STOMP
                connectWithStompCommand = true
            }

            session = StompClient(
                webSocketClient = wsClient,
                config = stompConfig
            ).connect(webSocketUrl)
                .withJsonConversions(json)
            println("Websocket url: $webSocketUrl")
//            session = KtorWebSocketClient(httpClient ).connect(webSocketUrl).withJsonConversions(json)

            _connectionState.value = _connectionState.value.copy(
                isConnected = true,
                connectionTime = connectionTime,
                messagesReceived = 0,
                lastError = null
            )

            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = "Failed to connect: ${e.message}"
            _connectionState.value = _connectionState.value.copy(
                isConnected = false,
                lastError = errorMessage,
                reconnectAttempts = _connectionState.value.reconnectAttempts + 1
            )
            Result.failure(e)
        }
    }

    /**
     * Subscribe to greenhouse messages topic
     * Automatically handles message counting and error recovery
     *
     * @return Flow of GreenhouseMessage objects from the server
     */
    suspend fun subscribeToMessages(): Flow<GreenhouseMessage> {
        val currentSession = session
            ?: throw IllegalStateException("Must connect() before subscribing")

        println("Session: $session")
        return currentSession.subscribeText("/topic/greenhouse/messages")
            .map { jsonString ->
                // Deserialize JSON string to GreenhouseMessage
                println("----Json----: $jsonString")
                json.decodeFromString<GreenhouseMessage>(jsonString)
            }
            .onStart {
                println("Starting subscription to /topic/greenhouse/messages")
            }
            .onEach { message ->
                // Update message count
                _connectionState.value = _connectionState.value.copy(
                    messagesReceived = _connectionState.value.messagesReceived + 1
                )
            }
            .catch { e ->
                val errorMessage = "WebSocket error: ${e.message}"
                println(errorMessage)
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    lastError = errorMessage
                )
                throw e // Re-throw to allow upstream handling
            }
            .onCompletion { cause ->
                if (cause != null) {
                    println("Subscription completed with error: ${cause.message}")
                } else {
                    println("Subscription completed normally")
                }
            }
    }

    /**
     * Disconnect from the WebSocket server
     * Cleans up the session and resets connection state
     */
    suspend fun disconnect() {
        try {
            session?.disconnect()
            session = null
            _connectionState.value = WebSocketConnectionState(
                isConnected = false
            )
        } catch (e: Exception) {
            println("Error during disconnect: ${e.message}")
            // Still reset state even if disconnect fails
            session = null
            _connectionState.value = _connectionState.value.copy(
                isConnected = false,
                lastError = "Disconnect error: ${e.message}"
            )
        }
    }

    /**
     * Attempt to reconnect with exponential backoff
     * Used for manual reconnection attempts
     *
     * @return Result indicating success or failure
     */
    suspend fun reconnect(): Result<Unit> {
        disconnect()
        delay(1000) // Wait 1 second before reconnecting
        return connect()
    }

    /**
     * Reset reconnect attempts counter
     * Call this after a successful connection to reset retry logic
     */
    fun resetReconnectAttempts() {
        _connectionState.value = _connectionState.value.copy(
            reconnectAttempts = 0
        )
    }
}
