package com.apptolast.greenhousefronts.util

enum class Environment(
    val baseUrl: String,
    val wsUrl: String,
) {
    DEV(
        baseUrl = "https://inverapi-dev.apptolast.com/api/v1",
        wsUrl = "wss://inverapi-dev.apptolast.com/ws/greenhouse/status/client",
    ),
    PROD(
        baseUrl = "https://inverapi-prod.apptolast.com/api/v1",
        wsUrl = "wss://inverapi-prod.apptolast.com/ws/greenhouse/status/client",
    );

    companion object {
        // Cambiar aquí para alternar entre entornos
        val current: Environment = DEV
    }
}