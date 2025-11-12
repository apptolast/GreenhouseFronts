package com.apptolast.greenhousefronts.util

import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatter

/**
 * iOS implementation of timestamp provider
 * Uses Foundation NSDate directly for better platform integration
 */
actual fun getCurrentTimestamp(): String {
    val formatter = NSISO8601DateFormatter()
    return formatter.stringFromDate(NSDate())
}
