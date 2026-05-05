package com.apptolast.greenhousefronts.data.remote.push

/**
 * Platform notification permission gate.
 *
 * Android 13+ (API 33+) and iOS both require user authorization before push notifications
 * are visible. Older Android versions and unsupported targets return `true` directly.
 *
 * The permission denial is non-fatal: token registration still proceeds so the user can
 * grant the permission later from the OS settings without re-logging in.
 */
interface PushNotifier {
    /**
     * Requests the OS notification permission if needed. Returns whether notifications can
     * currently be displayed. Implementations must not throw.
     */
    suspend fun ensurePermission(): Boolean
}

expect fun providePushNotifier(): PushNotifier
