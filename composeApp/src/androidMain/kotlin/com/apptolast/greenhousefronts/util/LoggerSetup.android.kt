package com.apptolast.greenhousefronts.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import co.touchlab.kermit.platformLogWriter

/**
 * Registers a `CrashlyticsLogWriter` alongside the default `platformLogWriter` (which
 * routes to Logcat). Breadcrumbs reach Firebase Crashlytics; ERROR-with-throwable becomes
 * a non-fatal report.
 */
actual fun configureCrashlyticsLogging() {
    Logger.setLogWriters(
        CrashlyticsLogWriter(),
        platformLogWriter(),
    )
}
