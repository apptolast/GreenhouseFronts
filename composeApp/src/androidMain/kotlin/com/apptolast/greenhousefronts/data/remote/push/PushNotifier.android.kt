package com.apptolast.greenhousefronts.data.remote.push

/**
 * On Android the runtime POST_NOTIFICATIONS dialog (API 33+) is launched from
 * `MainActivity.onCreate` via `ActivityResultContracts.RequestPermission`, because
 * that contract requires an `Activity`. From the registrar's point of view we always
 * proceed with the token registration: if the permission was denied the notifications
 * just won't be displayed, but the token is still valid the moment the user grants it
 * later from system settings.
 */
private object AndroidPushNotifier : PushNotifier {
    override suspend fun ensurePermission(): Boolean = true
}

actual fun providePushNotifier(): PushNotifier = AndroidPushNotifier
