package com.apptolast.greenhousefronts.data.remote.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.apptolast.greenhousefronts.MainActivity
import com.apptolast.greenhousefronts.R
import com.apptolast.greenhousefronts.data.local.notification.AlertNotificationSettings
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.graphics.toColorInt

/**
 * Receives Firebase Cloud Messaging events on Android.
 *
 *  - [onNewToken] forwards rotated tokens to [AndroidPushTokenBus] so [PushTokenRegistrar]
 *    can re-register them on the backend.
 *  - [onMessageReceived] is invoked when the app is in the foreground (or, for data-only
 *    payloads, in the background too). The backend sends both `notification` and `data`
 *    fields, so for visible-while-foreground delivery we materialise the `notification`
 *    block ourselves into a system notification so the user always sees it.
 *
 * When the user taps the notification, [MainActivity] is launched with extras carrying
 * the alert deep-link payload.
 */
class GreenhouseFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "===== NEW FCM TOKEN =====")
        Log.i(TAG, token)
        Log.i(TAG, "=========================")
        AndroidPushTokenBus.flow.tryEmit(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "Message received: data=${message.data}, notification=${message.notification?.title}")

        val alertSettings = AlertNotificationSettings()
        if (!alertSettings.alertsEnabled) {
            Log.i(TAG, "Alert notifications disabled — skipping display")
            return
        }

        val incomingSeverity = AlertSeverity.fromName(message.data["severity"])
        if (incomingSeverity != null) {
            val minLevel = AlertSeverity.entries.firstOrNull {
                it.level.toInt() == alertSettings.minSeverityLevel
            } ?: AlertSeverity.INFO
            if (incomingSeverity.level < minLevel.level) {
                Log.i(TAG, "Severity ${incomingSeverity.name} below minimum ${minLevel.name} — skipping display")
                return
            }
        }

        ensureChannel(this)

        val data = message.data
        val notif = message.notification
        val title = notif?.title ?: data["title"] ?: "Alerta"
        val body = notif?.body ?: data["body"] ?: data["alertCode"] ?: "Nueva alerta recibida"

        // Stable 31-bit positive id derived from the alertId Long. Avoids the Long→Int
        // overflow that the previous `toIntOrNull()` had — IDs above Int.MAX_VALUE silently
        // produced collisions or null. The masked low-31 bits guarantee positive Int and
        // 1:1 mapping for any alert id below 2^31, which we won't realistically hit.
        val alertIdLong = data["alertId"]?.toLongOrNull()
        val notificationId = alertIdLong?.toNotificationId()
            ?: (System.currentTimeMillis() and 0x7FFFFFFFL).toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = INTENT_ACTION_OPEN_ALERT
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data["greenhouseId"]?.let { putExtra(EXTRA_GREENHOUSE_ID, it) }
            data["sectorId"]?.let { putExtra(EXTRA_SECTOR_ID, it) }
            data["alertId"]?.let { putExtra(EXTRA_ALERT_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Backend ships the severity colour in `notification.color` (e.g. "#FFA500"). When
        // the system draws the notification (background delivery) it uses it automatically;
        // when WE draw it (foreground delivery, here) we have to pass it explicitly.
        // Fallback chain: backend hex → local mapping by severity name → theme accent.
        val accentColor = notif?.color?.tryParseColor()
            ?: data["severity"]?.let { localSeverityColor(it) }
            ?: DEFAULT_NOTIFICATION_COLOR

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            //.setSmallIcon(R.mipmap.ic_launcher)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setColor(accentColor)
            .setColorized(true)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    companion object {
        private const val TAG = "FCM"
        const val CHANNEL_ID = "alerts_default"
        const val INTENT_ACTION_OPEN_ALERT = "com.apptolast.greenhousefronts.OPEN_ALERT"
        const val EXTRA_GREENHOUSE_ID = "greenhouseId"
        const val EXTRA_SECTOR_ID = "sectorId"
        const val EXTRA_ALERT_ID = "alertId"

        // Mirror of metadata.alert_severities on the backend (V31 seed).
        private val DEFAULT_NOTIFICATION_COLOR = "#00E676".toColorInt()

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notificaciones de alertas del invernadero"
                enableLights(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        private fun Long.toNotificationId(): Int = (this and 0x7FFFFFFFL).toInt()

        private fun String.tryParseColor(): Int? =
            runCatching { this.toColorInt() }.getOrNull()

        private fun localSeverityColor(name: String): Int? = when (name.uppercase()) {
            "INFO" -> "#0066FF".toColorInt()
            "WARNING" -> "#FFA500".toColorInt()
            "ERROR" -> "#FF7722".toColorInt()
            "CRITICAL" -> "#FF0000".toColorInt()
            else -> null
        }
    }
}
