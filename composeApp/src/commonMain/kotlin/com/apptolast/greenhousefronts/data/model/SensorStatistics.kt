package com.apptolast.greenhousefronts.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * DTO for historical sensor statistics from the backend API.
 * Represents the response from /api/statistics/historical-data endpoint.
 *
 * @property greenhouseId UUID of the greenhouse
 * @property tenantId UUID of the tenant (optional)
 * @property sensorType Type of sensor (e.g., "TEMPERATURE", "HUMIDITY")
 * @property unit Unit of measurement (e.g., "°C", "%")
 * @property currentValue The most recent sensor reading
 * @property currentValueTimestamp Timestamp of the current value (ISO 8601)
 * @property avgValue Average value over the period
 * @property minValue Minimum value over the period
 * @property maxValue Maximum value over the period
 * @property medianValue Median value over the period
 * @property trendPercent Trend percentage change (e.g., 1.2 for +1.2%)
 * @property trendDirection Trend direction: "INCREASING", "DECREASING", or "STABLE"
 * @property chartData Array of data points for the chart
 * @property period Time period of the data (e.g., "24h", "7d", "30d")
 * @property startTime Start timestamp of the period (ISO 8601)
 * @property endTime End timestamp of the period (ISO 8601)
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class SensorStatistics(
    val greenhouseId: Uuid,
    val tenantId: Uuid,
    val sensorType: String = "",
    val unit: String = "",
    val currentValue: Double = 0.0,
    val currentValueTimestamp: String = "",
    val avgValue: Double = 0.0,
    val minValue: Double = 0.0,
    val maxValue: Double = 0.0,
    val medianValue: Double = 0.0,
    val trendPercent: Double = 0.0,
    val trendDirection: String = "",
    val chartData: List<ChartDataPoint> = emptyList(),
    val period: String = "",
    val startTime: String = "",
    val endTime: String = ""
)
