package com.apptolast.greenhousefronts.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * JVM/Desktop implementation of timestamp provider
 * Uses kotlin.time.Clock from the Kotlin standard library
 */
@OptIn(ExperimentalTime::class)
actual fun getCurrentTimestamp(): String {
    return Clock.System.now().toString()
}
