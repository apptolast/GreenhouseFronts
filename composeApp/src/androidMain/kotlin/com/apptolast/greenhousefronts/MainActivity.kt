package com.apptolast.greenhousefronts

import android.Manifest
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.apptolast.greenhousefronts.data.remote.push.AlertDeepLink
import com.apptolast.greenhousefronts.data.remote.push.AlertDeepLinkBus
import com.apptolast.greenhousefronts.data.remote.push.GreenhouseFcmService
import com.apptolast.greenhousefronts.presentation.ui.App

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored: best effort */ }

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

        GreenhouseFcmService.ensureChannel(this)
        maybeRequestNotificationPermission()
        handleAlertIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        logInboundIntent(intent)
        handleAlertIntent(intent)
    }

    private fun handleAlertIntent(intent: Intent?) {
        if (intent?.action != GreenhouseFcmService.INTENT_ACTION_OPEN_ALERT) return
        // alertId is the only mandatory field — without it the Alerts screen has nothing
        // to highlight. greenhouseId/sectorId travel along but the screen no longer needs them.
        val alertId = intent.getStringExtra(GreenhouseFcmService.EXTRA_ALERT_ID)?.toLongOrNull() ?: return
        val greenhouseId = intent.getStringExtra(GreenhouseFcmService.EXTRA_GREENHOUSE_ID)?.toLongOrNull()
        val sectorId = intent.getStringExtra(GreenhouseFcmService.EXTRA_SECTOR_ID)?.toLongOrNull()
        AlertDeepLinkBus.emit(
            AlertDeepLink(
                alertId = alertId,
                greenhouseId = greenhouseId,
                sectorId = sectorId,
            ),
        )
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
