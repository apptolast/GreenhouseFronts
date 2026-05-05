package com.apptolast.greenhousefronts.data.remote.push

private object DesktopPushNotifier : PushNotifier {
    override suspend fun ensurePermission(): Boolean = false
}

actual fun providePushNotifier(): PushNotifier = DesktopPushNotifier
