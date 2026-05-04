package com.apptolast.greenhousefronts.data.model.alert

import kotlinx.serialization.Serializable

/**
 * Generic page wrapper returned by `/alert-events` and `/alert-events/episodes`.
 * `total` is the count of all rows matching the filters across every page.
 */
@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
    val hasMore: Boolean,
)

/**
 * Actor that caused an alert state transition. `kind` is one of `USER` / `DEVICE` / `SYSTEM`.
 *
 *  - USER → manual action via REST. Carries `userId` (always) plus `username` /
 *    `displayName` when the backend hydrated them from the users table.
 *  - DEVICE → automatic transition coming from a sensor over MQTT. Optional `ref`.
 *  - SYSTEM → backend-internal automatic action (rules, jobs).
 */
@Serializable
data class AlertActorResponse(
    val kind: String,
    val userId: Long? = null,
    val username: String? = null,
    val displayName: String? = null,
    val ref: String? = null,
)

/**
 * One row from `GET /api/v1/tenants/{tenantId}/alert-events`. Each row is a single state
 * change of an alert — the same `alertId` may appear multiple times within one page if it
 * was opened, resolved and reopened inside the time window.
 *
 * The `severityColor` / `previousTransitionAt` / `episode*` / `occurrenceNumber` /
 * `totalTransitionsSoFar` fields come hydrated from the read path; on real-time STOMP
 * broadcasts these are best-effort sentinels (per backend doc).
 */
@Serializable
data class AlertTransitionResponse(
    val transitionId: Long,
    val at: String,
    val fromResolved: Boolean = false,
    val toResolved: Boolean,
    val source: String,
    val rawValue: String? = null,
    val actor: AlertActorResponse,
    val alertId: Long,
    val alertCode: String,
    val alertMessage: String? = null,
    val alertTypeId: Short? = null,
    val alertTypeName: String? = null,
    val severityId: Short? = null,
    val severityName: String? = null,
    val severityLevel: Short? = null,
    val severityColor: String? = null,
    val sectorId: Long,
    val sectorCode: String? = null,
    val greenhouseId: Long? = null,
    val greenhouseName: String? = null,
    val tenantId: Long? = null,
    val previousTransitionAt: String? = null,
    val episodeStartedAt: String? = null,
    val episodeDurationSeconds: Long? = null,
    val occurrenceNumber: Long = 0,
    val totalTransitionsSoFar: Long = 0,
)
