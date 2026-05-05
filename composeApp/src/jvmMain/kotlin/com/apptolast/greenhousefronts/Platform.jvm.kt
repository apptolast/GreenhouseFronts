package com.apptolast.greenhousefronts

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val versionName: String = System.getProperty("jpackage.app-version") ?: "1.0.0"
}

actual fun getPlatform(): Platform = JVMPlatform()