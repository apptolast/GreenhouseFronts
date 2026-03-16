package com.apptolast.greenhousefronts.util

enum class Environment(val baseUrl: String) {
    DEV("https://inverapi-dev.apptolast.com/api/v1"),
    PROD("https://inverapi-prod.apptolast.com/api/v1");

    companion object {
        // Cambiar aquí para alternar entre entornos
        val current: Environment = DEV
    }
}