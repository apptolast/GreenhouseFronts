package com.apptolast.greenhousefronts

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.apptolast.greenhousefronts.di.initKoin
import com.apptolast.greenhousefronts.presentation.ui.App
import com.apptolast.greenhousefronts.util.ExternalUriHandler
import java.awt.Desktop

fun main(args: Array<String>) {
    // Initialize Koin before creating UI
    initKoin()

    if (System.getProperty("os.name").indexOf("Mac") > -1) {
        Desktop.getDesktop().setOpenURIHandler { uri ->
            print("URI: ${uri.uri}")
            ExternalUriHandler.onNewUri(uri.uri.toString())
        }
    } else {
        print("Args: ${args.getOrNull(0)}")
        ExternalUriHandler.onNewUri(args.getOrNull(0).toString())
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GreenhouseFronts",
        ) {
            App()
        }
    }
}