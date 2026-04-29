package com.apptolast.greenhousefronts.util

/**
 * Defensive parsers for the boolean wire format.
 *
 * The backend (after the Phase 6 normalisation) sends `"true"` / `"false"` for
 * BOOLEAN-typed `currentValue` fields, but historically also stored `"1"` / `"0"`
 * (the format `DeviceStatusListener` writes to TimescaleDB). To stay tolerant of
 * either format — and resilient against future backend revisions or older
 * snapshots — every place that interprets a boolean from a `String?` should go
 * through these helpers instead of comparing literals.
 *
 * Empty string and `null` are treated as the absence of a value, never as
 * `false`, so callers can distinguish "no data yet" from "off".
 */
fun String?.isTrueLike(): Boolean {
    val v = this ?: return false
    return v.equals("true", ignoreCase = true) || v == "1"
}

fun String?.isFalseLike(): Boolean {
    val v = this ?: return false
    return v.equals("false", ignoreCase = true) || v == "0"
}
