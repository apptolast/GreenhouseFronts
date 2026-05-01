package com.apptolast.greenhousefronts.data.remote.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private object AndroidPushTokenProvider : PushTokenProvider {
    override val platform: PushPlatform = PushPlatform.ANDROID

    override suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    override fun tokenUpdates(): Flow<String> = AndroidPushTokenBus.flow
}

actual fun providePushTokenProvider(): PushTokenProvider = AndroidPushTokenProvider
