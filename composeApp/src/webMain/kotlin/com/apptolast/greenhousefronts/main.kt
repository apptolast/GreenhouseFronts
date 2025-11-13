package com.apptolast.greenhousefronts

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.apptolast.greenhousefronts.di.initKoin
import com.apptolast.greenhousefronts.presentation.ui.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize Koin before creating UI
    initKoin()

    ComposeViewport {
        App()
    }
}