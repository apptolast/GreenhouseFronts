package com.apptolast.greenhousefronts.data.remote.push

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Single-shot bus for alert deep links produced by tapping an FCM notification.
 *
 * Producers (Android `MainActivity`, iOS `IOSPushBridge`, future Web SW handler) call
 * [emit]; the consumer (`App.kt`) collects [events] and navigates exactly once.
 *
 * Backed by a `Channel` (not a `StateFlow`) so each tap navigates once and isn't replayed
 * on configuration changes.
 */
object AlertDeepLinkBus {
    private val channel = Channel<AlertDeepLink>(capacity = Channel.BUFFERED)
    val events: Flow<AlertDeepLink> = channel.receiveAsFlow()

    fun emit(link: AlertDeepLink) {
        channel.trySend(link)
    }

    /**
     * Parse the FCM `data` map produced by the backend's `AlertPushPayload` and emit it.
     * Silently ignores malformed payloads — we never want push handling to crash the app.
     *
     * Required: [AlertDeepLink.alertId]. Everything else is opportunistic context.
     */
    fun emitFromData(data: Map<String, String>) {
        val alertId = data["alertId"]?.toLongOrNull() ?: return
        emit(
            AlertDeepLink(
                alertId = alertId,
                greenhouseId = data["greenhouseId"]?.toLongOrNull(),
                sectorId = data["sectorId"]?.toLongOrNull(),
                alertCode = data["alertCode"],
                severityName = data["severity"],
                severityLevel = data["severityLevel"]?.toShortOrNull(),
                createdAtEpochMs = data["createdAt"]?.toLongOrNull(),
            ),
        )
    }
}
