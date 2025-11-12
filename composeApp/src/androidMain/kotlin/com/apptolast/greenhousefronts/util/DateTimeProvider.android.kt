package com.apptolast.greenhousefronts.util

import kotlinx.datetime.Clock

/**
 * Implementación Android del proveedor de timestamp
 * Usa kotlinx-datetime que funciona correctamente en Android
 */
actual fun getCurrentTimestamp(): String {
    return Clock.System.now().toString()
}
