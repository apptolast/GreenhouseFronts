package com.apptolast.greenhousefronts

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.apptolast.greenhousefronts.di.initKoin
import com.apptolast.greenhousefronts.presentation.ui.App

fun main() {
    // Initialize Koin before creating UI
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GreenhouseFronts",
        ) {
            App()
        }
    }
}