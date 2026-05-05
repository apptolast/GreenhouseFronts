package com.apptolast.greenhousefronts.data.remote.push

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Desktop has no first-party FCM SDK — push notifications are not delivered when the JVM
 * process is stopped, so we keep the surface as a no-op. The registrar will see a null
 * token and skip the backend call.
 */
private object DesktopPushTokenProvider : PushTokenProvider {
    override val platform: PushPlatform = PushPlatform.DESKTOP
    override suspend fun currentToken(): String? = null
    override fun tokenUpdates(): Flow<String> = emptyFlow()
}

actual fun providePushTokenProvider(): PushTokenProvider = DesktopPushTokenProvider
