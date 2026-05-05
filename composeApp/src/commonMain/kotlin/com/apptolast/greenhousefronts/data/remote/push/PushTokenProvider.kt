package com.apptolast.greenhousefronts.data.remote.push

import kotlinx.coroutines.flow.Flow

/**
 * Source of the FCM (or platform-equivalent) push registration token.
 *
 * Implementations are platform-specific:
 *  - Android: FirebaseMessaging.getInstance().token + onNewToken bridge
 *  - iOS: forwarded from the Swift `MessagingDelegate` via [com.apptolast.greenhousefronts.data.remote.push.IOSPushBridge]
 *  - JVM/Web: no-ops (return null / never emit) — FCM is not supported there in a useful way
 *
 * The [platform] field is sent verbatim to the backend so the same registry table can hold
 * tokens from heterogeneous clients.
 */
interface PushTokenProvider {
    val platform: PushPlatform

    /** Returns the most recent token, or null if FCM has not produced one (or platform unsupported). */
    suspend fun currentToken(): String?

    /** Emits whenever the underlying SDK rotates the token. */
    fun tokenUpdates(): Flow<String>
}

expect fun providePushTokenProvider(): PushTokenProvider
