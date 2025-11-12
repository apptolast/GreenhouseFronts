package com.apptolast.greenhousefronts.util

import kotlinx.datetime.Clock

/**
 * Implementación WebAssembly del proveedor de timestamp
 * Usa kotlinx-datetime que funciona correctamente en Wasm
 */
actual fun getCurrentTimestamp(): String {
    return Clock.System.now().toString()
}
