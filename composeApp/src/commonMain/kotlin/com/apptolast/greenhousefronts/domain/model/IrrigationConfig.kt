package com.apptolast.greenhousefronts.domain.model

/**
 * Domain model for the irrigation configuration screen.
 */
data class IrrigationConfig(
    val greenhouseId: Long,
    val greenhouseName: String,
    val isIrrigating: Boolean = false,
    val irrigationStatus: String? = null,
    val isInQueue: Boolean = false,
    val queueStatus: String? = null,
    val activeDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    ),
    val startHour: Int = 11,
    val startMinute: Int = 30,
    val endHour: Int = 18,
    val endMinute: Int = 30,
    val waitBetweenMinutes: Int = 90,
    val sectorConfigs: List<SectorIrrigationConfig> = emptyList(),
)

data class SectorIrrigationConfig(
    val sectorId: Long,
    val sectorName: String,
    val openingMinutes: Int = 0,
    val waitMinutes: Int = 4,
    val isActive: Boolean = false,
)

enum class DayOfWeek(val label: String, val shortLabel: String) {
    MONDAY("Lunes", "L"),
    TUESDAY("Martes", "M"),
    WEDNESDAY("Miércoles", "X"),
    THURSDAY("Jueves", "J"),
    FRIDAY("Viernes", "V"),
    SATURDAY("Sábado", "S"),
    SUNDAY("Domingo", "D"),
}
