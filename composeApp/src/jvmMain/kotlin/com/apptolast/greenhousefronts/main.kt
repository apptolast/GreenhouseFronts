package com.apptolast.greenhousefronts

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.apptolast.greenhousefronts.presentation.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GreenhouseFronts",
    ) {
        App()
    }
}