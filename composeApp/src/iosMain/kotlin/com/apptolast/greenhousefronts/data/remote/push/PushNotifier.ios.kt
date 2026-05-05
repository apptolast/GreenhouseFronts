package com.apptolast.greenhousefronts.data.remote.push

/**
 * On iOS the authorization request is issued from the Swift `AppDelegate`
 * (`UNUserNotificationCenter.requestAuthorization`) at app launch. Re-asking from Kotlin
 * would be redundant and would race with the Swift side.
 *
 * Returning `true` here just lets the registrar proceed with the token registration; the
 * iOS OS will silently drop notifications if the user denied them — same behaviour the
 * registrar expects on Android.
 */
private object IosPushNotifier : PushNotifier {
    override suspend fun ensurePermission(): Boolean = true
}

actual fun providePushNotifier(): PushNotifier = IosPushNotifier
