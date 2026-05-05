package com.apptolast.greenhousefronts.data.remote.push

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Web FCM is intentionally not wired yet. It would require the Firebase JS SDK plus a
 * service worker (`firebase-messaging-sw.js`) at the site root. This stub keeps the
 * KMP build green for js / wasmJs targets; flipping it to a real implementation later
 * will only touch this file.
 */
private object WebPushTokenProvider : PushTokenProvider {
    override val platform: PushPlatform = PushPlatform.WEB
    override suspend fun currentToken(): String? = null
    override fun tokenUpdates(): Flow<String> = emptyFlow()
}

actual fun providePushTokenProvider(): PushTokenProvider = WebPushTokenProvider
