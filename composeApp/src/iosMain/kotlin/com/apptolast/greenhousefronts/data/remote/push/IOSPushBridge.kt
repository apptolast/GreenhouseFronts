package com.apptolast.greenhousefronts.data.remote.push

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Bridge between the iOS Swift side (AppDelegate / FirebaseMessaging delegate) and the
 * Kotlin/Native runtime.
 *
 * Kotlin/Native cannot link against `FirebaseMessaging` directly in this project (no
 * cinterop or pod is configured for it), so the iOS Firebase SDK runs entirely on the
 * Swift side and forwards data into Kotlin via the static functions on this object.
 *
 * The matching Swift code lives in `iosApp/iosApp/AppDelegate.swift`.
 */
object IOSPushBridge {
    /** Replays the most recent token so the registrar can read it on startup. */
    private val tokenStateFlow = MutableStateFlow<String?>(null)

    /** Hot stream of token rotations forwarded to [PushTokenRegistrar]. */
    private val tokenUpdatesFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)

    /** Called from Swift when `MessagingDelegate.didReceiveRegistrationToken` fires. */
    fun pushNewToken(token: String) {
        if (token.isBlank()) return
        tokenStateFlow.value = token
        tokenUpdatesFlow.tryEmit(token)
    }

    /** Called from Swift when the user taps a push notification. Map keys come from FCM `data`. */
    fun handleAlertDeepLink(payload: Map<String, String>) {
        AlertDeepLinkBus.emitFromData(payload)
    }

    internal fun currentToken(): String? = tokenStateFlow.value
    internal fun updates(): kotlinx.coroutines.flow.Flow<String> = tokenUpdatesFlow
}
