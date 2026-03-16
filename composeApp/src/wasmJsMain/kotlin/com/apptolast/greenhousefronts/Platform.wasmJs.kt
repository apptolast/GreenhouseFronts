package com.apptolast.greenhousefronts

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val versionName: String = "1.0.0"
}

actual fun getPlatform(): Platform = WasmPlatform()