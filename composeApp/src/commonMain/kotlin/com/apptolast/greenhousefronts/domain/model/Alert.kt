package com.apptolast.greenhousefronts.domain.model

/**
 * Domain representation of an alert as shown to the user. Mapped from
 * `AlertResponse` (REST DTO) — keeps the timestamps as strings on purpose so the parsing
 * (kotlinx-datetime ISO-8601) lives next to the formatter in the UI layer.
 */
data class Alert(
    val id: Long,
    val code: String,
    val sectorId: Long,
    val sectorCode: String?,
    val alertTypeName: String?,
    val severityName: String,
    val severityLevel: Short,
    val message: String?,
    val description: String?,
    val clientName: String?,
    val isResolved: Boolean,
    val resolvedAt: String?,
    val resolvedByUserName: String?,
    val createdAt: String,
    val updatedAt: String,
) {
    val severity: AlertSeverity
        get() = AlertSeverity.fromName(severityName) ?: AlertSeverity.INFO
}

/**
 * Fixed catalogue of alert severities. Hex values mirror
 * `metadata.alert_severities` from the backend seed (V31__seed_all_initial_data.sql)
 * so notification banners and chips stay consistent across surfaces.
 *
 * If the backend ever introduces a severity outside this list, the mapper falls back to
 * INFO and the rest of the UI keeps working.
 */
enum class AlertSeverity(
    val level: Short,
    val display: String,
    val colorHex: String,
) {
    INFO(level = 1, display = "Info", colorHex = "#0066FF"),
    WARNING(level = 2, display = "Aviso", colorHex = "#FFA500"),
    ERROR(level = 3, display = "Error", colorHex = "#FF7722"),
    CRITICAL(level = 4, display = "Crítico", colorHex = "#FF0000");

    companion object {
        fun fromName(name: String?): AlertSeverity? {
            if (name == null) return null
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
    }
}
