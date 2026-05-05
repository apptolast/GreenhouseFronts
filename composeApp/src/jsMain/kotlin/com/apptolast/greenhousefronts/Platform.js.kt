package com.apptolast.greenhousefronts

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
    override val versionName: String = "1.0.0"
}

actual fun getPlatform(): Platform = JsPlatform()