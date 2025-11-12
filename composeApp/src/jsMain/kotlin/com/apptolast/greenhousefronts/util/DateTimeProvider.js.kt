package com.apptolast.greenhousefronts.util

import kotlinx.datetime.Clock

/**
 * Implementación JavaScript del proveedor de timestamp
 * Usa kotlinx-datetime que funciona correctamente en JS
 */
actual fun getCurrentTimestamp(): String {
    return Clock.System.now().toString()
}
