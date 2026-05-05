package com.apptolast.greenhousefronts.domain.model

/**
 * One state change in an alert's life. Distinct from [Alert] (which is "an alert as an
 * entity, with its current state"): a transition is "this is what happened at instant T".
 *
 * The same `alertId` may produce many transitions over time (open → resolve → reopen → …).
 */
data class AlertTransition(
    /** Unique row id — used as LazyColumn key, since `alertId` may repeat in the page. */
    val transitionId: Long,
    /** ISO-8601 timestamp of the change. */
    val at: String,
    /** State after the change. `true` = the alert is now resolved. */
    val toResolved: Boolean,
    /** Where the change came from: MQTT (sensor), API (REST), or SYSTEM (backend job). */
    val source: String,
    /** Who/what caused it. */
    val actor: AlertActor,
    val alertId: Long,
    val alertCode: String,
    val alertMessage: String?,
    val alertTypeName: String?,
    val severityName: String,
    val severityLevel: Short,
    val sectorId: Long,
    val sectorCode: String?,
    /** "How many times this alert had opened up to and including this transition." */
    val occurrenceNumber: Long,
) {
    val severity: AlertSeverity
        get() = AlertSeverity.fromName(severityName) ?: AlertSeverity.INFO
}

/**
 * Person, device or backend that triggered an alert transition. Discriminated by [kind].
 */
data class AlertActor(
    val kind: ActorKind,
    val userId: Long? = null,
    val username: String? = null,
    val displayName: String? = null,
    /** Free-form device reference (e.g. gateway id) when [kind] is [ActorKind.DEVICE]. */
    val ref: String? = null,
) {
    enum class ActorKind { USER, DEVICE, SYSTEM, UNKNOWN }

    /** Human-readable label for the UI. */
    val label: String
        get() = when (kind) {
            ActorKind.USER -> displayName?.takeIf { it.isNotBlank() }
                ?: username?.takeIf { it.isNotBlank() }
                ?: userId?.let { "Usuario #$it" }
                ?: "Usuario"

            ActorKind.DEVICE -> ref?.takeIf { it.isNotBlank() }?.let { "Sensor $it" } ?: "Sensor"
            ActorKind.SYSTEM -> "Sistema"
            ActorKind.UNKNOWN -> "—"
        }
}

/** Domain wrapper for paginated REST responses, decoupled from the DTO layer. */
data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
    val hasMore: Boolean,
)
