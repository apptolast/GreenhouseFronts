package com.apptolast.greenhousefronts.data.model.sensor

import kotlinx.serialization.Serializable

/**
 * Response from GET /api/v1/statistics/historical-data
 * Contains pre-aggregated chart data and statistics from TimescaleDB continuous aggregates.
 * The backend uses an adaptive algorithm to choose optimal resolution per sensor.
 */
@Serializable
data class HistoricalDataResponse(
    val code: String,
    val unit: String? = null,
    val currentValue: Double? = null,
    val currentValueTimestamp: String? = null,
    val avgValue: Double? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val trendPercent: Double? = null,
    val trendDirection: String? = null,
    val chartData: List<ChartDataPoint> = emptyList(),
    val period: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    // Adaptive resolution metadata
    val resolution: String? = null,
    val pointCount: Int = 0,
    // Boolean device data (REGANDO, EN COLA, etc.)
    val isBooleanDevice: Boolean = false,
    val transitions: List<TransitionPointDto>? = null,
    val booleanStats: BooleanStatsResponseDto? = null,
)

@Serializable
data class ChartDataPoint(
    val timestamp: String,
    val value: Double,
    val minValue: Double? = null,
    val maxValue: Double? = null,
)

@Serializable
data class TransitionPointDto(
    val timestamp: String,
    val newState: Boolean,
)

@Serializable
data class BooleanStatsResponseDto(
    val transitionCount: Int,
    val onPercentage: Double,
    val offPercentage: Double,
)
