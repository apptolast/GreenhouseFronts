package com.apptolast.greenhousefronts.data.remote.push

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Static bridge that lets [GreenhouseFcmService.onNewToken] forward rotated tokens to
 * the Kotlin coroutine flow consumed by [PushTokenRegistrar]. The service is created by
 * Android (not by Koin), so we cannot inject the registrar directly.
 */
internal object AndroidPushTokenBus {
    val flow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
}
