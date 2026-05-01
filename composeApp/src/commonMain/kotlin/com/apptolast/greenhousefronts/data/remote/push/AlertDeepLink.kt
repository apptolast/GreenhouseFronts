package com.apptolast.greenhousefronts.data.remote.push

/**
 * Deep link payload extracted from an alert push notification.
 *
 * Mirrors the `data` map shipped by the backend `AlertPushPayload` (see
 * `apptolast/InvernaderosAPI` PR #104). The only required field is [alertId] — all the
 * others are best-effort context for the UI: the Alerts screen looks the alert up by id and
 * uses the rest as a fallback while the network round-trip is in flight.
 */
data class AlertDeepLink(
    val alertId: Long,
    val greenhouseId: Long? = null,
    val sectorId: Long? = null,
    val alertCode: String? = null,
    val severityName: String? = null,
    val severityLevel: Short? = null,
    val createdAtEpochMs: Long? = null,
)
