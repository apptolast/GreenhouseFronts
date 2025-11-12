package com.apptolast.greenhousefronts.util

enum class Environment(val baseUrl: String) {
    DEV("https://inverapi-dev.apptolast.com"),
    PROD("https://inverapi-prod.apptolast.com");

    companion object {
        // Cambiar aquí para alternar entre entornos
        val current: Environment = DEV
    }
}