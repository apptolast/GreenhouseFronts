package com.apptolast.greenhousefronts.util

/**
 * Renders a `Device.currentValue` as the string shown in cards and detail screens.
 *
 * The `dataType` parameter (from `Device.dataType`, which mirrors the WS
 * `WsDeviceTypeDto.dataType`) is the *only* signal used to decide whether the value
 * is boolean. We never infer "boolean-ness" from the raw string — otherwise numeric
 * sensors that legitimately read `0` (wind speed, UV index, lux, …) get rendered as
 * "OFF". A null `dataType` is treated as non-boolean, which is the safer default for
 * this product (mostly numeric sensors; missing metadata more likely than legacy
 * untyped actuators).
 *
 * Numeric formatting:
 *  - values ≥ 10 000 are abbreviated to `45K`
 *  - whole-number doubles drop the trailing `.0`
 *  - everything else is returned verbatim
 */
fun formatDeviceValue(value: String?, dataType: String?): String {
    if (value == null) return "--"
    if (dataType.equals("BOOLEAN", ignoreCase = true)) {
        if (value.isTrueLike()) return "ON"
        if (value.isFalseLike()) return "OFF"
        // BOOLEAN-typed but unrecognised payload — fall through to raw rendering
    }
    val numValue = value.toDoubleOrNull()
    if (numValue != null && numValue >= 10000) return "${(numValue / 1000).toInt()}K"
    if (numValue != null && numValue == numValue.toLong().toDouble()) return numValue.toLong().toString()
    return value
}
