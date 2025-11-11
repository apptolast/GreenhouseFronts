package com.apptolast.greenhousefronts

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform