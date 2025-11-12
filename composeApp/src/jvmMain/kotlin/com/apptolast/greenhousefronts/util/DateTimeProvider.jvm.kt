package com.apptolast.greenhousefronts.util

import kotlinx.datetime.Clock

/**
 * Implementación JVM/Desktop del proveedor de timestamp
 * Usa kotlinx-datetime que funciona correctamente en JVM
 */
actual fun getCurrentTimestamp(): String {
    return Clock.System.now().toString()
}
