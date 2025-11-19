package com.apptolast.greenhousefronts.util

import com.apptolast.greenhousefronts.data.model.ChartDataPoint
import com.apptolast.greenhousefronts.data.model.TimePeriod
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Generates mock chart data for testing and development purposes.
 * Creates realistic data points with appropriate intervals for each time period.
 *
 * @param period The time period for the data generation
 * @param baseValue The base value around which to generate random variations
 * @return List of chart data points with timestamps and values
 */
@OptIn(ExperimentalTime::class)
fun generateMockData(period: TimePeriod, baseValue: Double = 15.0): List<ChartDataPoint> {
    val now = kotlin.time.Clock.System.now()
    val dataPoints = mutableListOf<ChartDataPoint>()

    when (period) {
        TimePeriod.LAST_24H -> {
            // Generate 48 points (one every 30 minutes for 24h)
            repeat(48) { index ->
                val timestamp = now.minus(24.hours - (index * 30).minutes)
                val value = baseValue + Random.nextDouble(-10.0, 10.0)
                dataPoints.add(
                    ChartDataPoint(
                        timestamp = timestamp.toEpochMilliseconds().toString(),
                        value = value
                    )
                )
            }
        }

        TimePeriod.LAST_7D -> {
            // Generate 28 points (one every 6 hours for 7 days)
            repeat(28) { index ->
                val timestamp = now.minus(7.days - (index * 6).hours)
                val value = baseValue + Random.nextDouble(-10.0, 10.0)
                dataPoints.add(
                    ChartDataPoint(
                        timestamp = timestamp.toEpochMilliseconds().toString(),
                        value = value
                    )
                )
            }
        }

        TimePeriod.LAST_30D -> {
            // Generate 30 points (one per day for 30 days)
            repeat(30) { index ->
                val timestamp = now.minus(30.days - (index).days)
                val value = baseValue + Random.nextDouble(-10.0, 10.0)
                dataPoints.add(
                    ChartDataPoint(
                        timestamp = timestamp.toEpochMilliseconds().toString(),
                        value = value
                    )
                )
            }
        }
    }

    return dataPoints
}

/**
 * Formats timestamp for X-axis labels based on the selected time period.
 * Adapts the format to show the most relevant information for each period:
 * - LAST_24H: Hours and minutes (HH:mm)
 * - LAST_7D: Day of week abbreviation and day number (e.g., "Lun 15")
 * - LAST_30D: Day and month (DD/MM)
 *
 * @param timestampMillis The timestamp in milliseconds
 * @param period The time period context for formatting
 * @return Formatted string appropriate for the time period
 */
@OptIn(ExperimentalTime::class)
fun formatXAxisLabel(timestampMillis: Long, period: TimePeriod): String {
    val instant = Instant.fromEpochMilliseconds(timestampMillis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    return when (period) {
        TimePeriod.LAST_24H -> {
            // Show hours: "00:00", "06:00", "12:00", etc.
            "${dateTime.hour.toString().padStart(2, '0')}:${
                dateTime.minute.toString().padStart(2, '0')
            }"
        }

        TimePeriod.LAST_7D -> {
            // Show day abbreviation and day: "Lun 15", "Mar 16", etc.
            val dayOfWeek = when (dateTime.dayOfWeek) {
                DayOfWeek.MONDAY -> "Lun"
                DayOfWeek.TUESDAY -> "Mar"
                DayOfWeek.WEDNESDAY -> "Mié"
                DayOfWeek.THURSDAY -> "Jue"
                DayOfWeek.FRIDAY -> "Vie"
                DayOfWeek.SATURDAY -> "Sáb"
                DayOfWeek.SUNDAY -> "Dom"
                else -> ""
            }
            "$dayOfWeek ${dateTime.dayOfMonth}"
        }

        TimePeriod.LAST_30D -> {
            // Show day and month: "1/12", "5/12", "10/12", etc.
            "${dateTime.dayOfMonth}/${dateTime.monthNumber}"
        }
    }
}

/**
 * Selects a subset of timestamps for X-axis labels to avoid overcrowding.
 * Samples labels uniformly across the data range.
 *
 * @param timestamps List of all timestamps
 * @param maxLabels Maximum number of labels to display (default: 6)
 * @return List of pairs (index, formatted label) for selected timestamps
 */
fun selectXAxisLabels(
    timestamps: List<Long>,
    period: TimePeriod,
    maxLabels: Int = 6
): List<Pair<Int, String>> {
    if (timestamps.isEmpty()) return emptyList()
    if (timestamps.size <= maxLabels) {
        return timestamps.mapIndexed { index, timestamp ->
            index to formatXAxisLabel(timestamp, period)
        }
    }

    val step = timestamps.size / (maxLabels - 1)
    return buildList {
        for (i in 0 until maxLabels - 1) {
            val index = i * step
            add(index to formatXAxisLabel(timestamps[index], period))
        }
        // Always include the last timestamp
        val lastIndex = timestamps.size - 1
        add(lastIndex to formatXAxisLabel(timestamps[lastIndex], period))
    }
}
