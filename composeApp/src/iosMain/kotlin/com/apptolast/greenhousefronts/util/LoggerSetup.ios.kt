package com.apptolast.greenhousefronts.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import co.touchlab.kermit.platformLogWriter

/**
 * Wires Kermit to FirebaseCrashlytics-iOS via ObjC interop. The host iOS app must call
 * `FirebaseApp.configure()` from its AppDelegate before this runs — otherwise Crashlytics
 * silently no-ops.
 */
actual fun configureCrashlyticsLogging() {
    Logger.setLogWriters(
        CrashlyticsLogWriter(),
        platformLogWriter(),
    )
}
