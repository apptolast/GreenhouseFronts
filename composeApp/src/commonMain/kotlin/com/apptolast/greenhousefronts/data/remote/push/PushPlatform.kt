package com.apptolast.greenhousefronts.data.remote.push

/**
 * Identifier sent to the backend so it can address tokens by transport.
 * Matches the `platform` enum on the backend's `metadata.push_token` table.
 */
enum class PushPlatform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP,
}
