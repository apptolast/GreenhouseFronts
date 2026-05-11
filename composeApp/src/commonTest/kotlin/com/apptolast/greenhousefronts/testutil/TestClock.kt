package com.apptolast.greenhousefronts.testutil

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Mutable [Clock] for unit tests. The whole point of the auth-refresh refactor was to
 * inject a clock so tests can simulate "1h elapsed", "24h elapsed", etc. without sleeping.
 *
 * Not thread-safe: tests are expected to advance it from a single coroutine.
 */
@OptIn(ExperimentalTime::class)
class TestClock(initialEpochSeconds: Long = DEFAULT_NOW) : Clock {

    private var currentInstant: Instant = Instant.fromEpochSeconds(initialEpochSeconds)

    override fun now(): Instant = currentInstant

    /** Move the clock forward by [delta]. Negative values are accepted but discouraged. */
    fun advanceBy(delta: Duration) {
        currentInstant += delta
    }

    /** Convenience: jump to an exact epoch-second value. */
    fun setNow(epochSeconds: Long) {
        currentInstant = Instant.fromEpochSeconds(epochSeconds)
    }

    val nowEpochSeconds: Long get() = currentInstant.epochSeconds

    companion object {
        /** 2026-01-01T00:00:00Z — fixed reference point so test JWTs read consistently. */
        const val DEFAULT_NOW: Long = 1_767_225_600L
    }
}
