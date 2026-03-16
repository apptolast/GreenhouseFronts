package com.apptolast.greenhousefronts.data.remote.websocket

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.util.Environment
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.sendEmptyMsg
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * STOMP WebSocket client for requesting the full greenhouse status hierarchy.
 */
class GreenhouseStatusWebSocket(
    private val tokenStorage: TokenStorage,
    private val json: Json,
) {

    suspend fun requestStatus(): GreenhouseStatusResponse {
        val token = tokenStorage.getToken()
        val wsUrl = Environment.current.wsUrl

        println("$TAG requestStatus() -> URL: $wsUrl")
        println("$TAG Token present: ${token != null}, length: ${token?.length ?: 0}")

        val wsClient = KtorWebSocketClient()
        val stompClient = StompClient(wsClient)

        val headers = buildMap {
            if (token != null) {
                put("Authorization", "Bearer $token")
            }
        }
        println("$TAG STOMP headers: ${headers.keys}")

        println("$TAG Connecting to STOMP...")
        val session = try {
            stompClient.connect(wsUrl, customStompConnectHeaders = headers)
        } catch (e: Exception) {
            println("$TAG CONNECT FAILED: ${e::class.simpleName}: ${e.message}")
            throw e
        }
        println("$TAG STOMP CONNECTED successfully")

        return try {
            println("$TAG Subscribing to /user/queue/status/response...")
            val subscription = session.subscribeText("/user/queue/status/response")
            println("$TAG Subscribed. Sending request to /app/status/request...")
            session.sendEmptyMsg("/app/status/request")
            println("$TAG Request sent. Waiting for response...")

            val messageText = subscription.first()
            println("$TAG Response received! Length: ${messageText.length} chars")
            println("$TAG Response preview: ${messageText.take(500)}...")

            val parsed = json.decodeFromString<GreenhouseStatusResponse>(messageText)
            println("$TAG Parsed OK -> ${parsed.tenants.size} tenants")
            parsed.tenants.forEach { tenant ->
                println("$TAG   Tenant: ${tenant.name} (id=${tenant.id}), ${tenant.greenhouses.size} greenhouses")
                tenant.greenhouses.forEach { gh ->
                    println("$TAG     Greenhouse: ${gh.name} (id=${gh.id}), ${gh.sectors.size} sectors")
                    gh.sectors.forEach { sector ->
                        println("$TAG       Sector: ${sector.name} (id=${sector.id}), ${sector.devices.size} devices, ${sector.settings.size} settings")
                    }
                }
            }
            parsed
        } catch (e: Exception) {
            println("$TAG RECEIVE/PARSE FAILED: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            println("$TAG Disconnecting...")
            session.disconnect()
            println("$TAG Disconnected")
        }
    }

    companion object {
        private const val TAG = "[WS-GREENHOUSE]"
    }
}
