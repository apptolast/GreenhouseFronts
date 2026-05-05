package com.apptolast.greenhousefronts.domain.model

/**
 * Domain model representing a greenhouse with aggregated info.
 */
data class Greenhouse(
    val id: Long,
    val code: String,
    val name: String,
    val isActive: Boolean,
    val areaM2: Double?,
    val sectorCount: Int,
    val alertCount: Int,
    val sectorNames: List<String> = emptyList(),
)
