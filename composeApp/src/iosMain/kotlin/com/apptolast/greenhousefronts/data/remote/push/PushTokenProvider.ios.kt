package com.apptolast.greenhousefronts.data.remote.push

import kotlinx.coroutines.flow.Flow

private object IosPushTokenProvider : PushTokenProvider {
    override val platform: PushPlatform = PushPlatform.IOS
    override suspend fun currentToken(): String? = IOSPushBridge.currentToken()
    override fun tokenUpdates(): Flow<String> = IOSPushBridge.updates()
}

actual fun providePushTokenProvider(): PushTokenProvider = IosPushTokenProvider
