package com.apptolast.greenhousefronts.util

import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatter

/**
 * Implementación iOS del proveedor de timestamp
 * Usa Foundation NSDate directamente para evitar problemas de dependencias
 */
actual fun getCurrentTimestamp(): String {
    val formatter = NSISO8601DateFormatter()
    return formatter.stringFromDate(NSDate())
}
