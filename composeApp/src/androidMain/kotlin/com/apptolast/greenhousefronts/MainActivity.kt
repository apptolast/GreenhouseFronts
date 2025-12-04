package com.apptolast.greenhousefronts

import android.app.ComponentCaller
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apptolast.greenhousefronts.presentation.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Configure edge-to-edge with auto-adjusting status bar icons
        // SystemBarStyle.auto automatically switches icon colors based on dark/light mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        logInboundIntent(intent)
    }

    private fun logInboundIntent(intent: Intent) {
        val data = intent.data
        // Loggea la URI completa y el token
        android.util.Log.d("DEEPLINK", "URI: ${data?.toString()}")
        android.util.Log.d("DEEPLINK", "host=${data?.host} path=${data?.path} query=${data?.query}")
        android.util.Log.d("DEEPLINK", "token=${data?.getQueryParameter("token")}")
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}