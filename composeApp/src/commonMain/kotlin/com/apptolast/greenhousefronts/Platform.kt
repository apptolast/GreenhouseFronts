package com.apptolast.greenhousefronts

interface Platform {
    val name: String
    val versionName: String
}

expect fun getPlatform(): Platform