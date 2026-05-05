package com.apptolast.greenhousefronts.data.remote.push

private object WebPushNotifier : PushNotifier {
    override suspend fun ensurePermission(): Boolean = false
}

actual fun providePushNotifier(): PushNotifier = WebPushNotifier
