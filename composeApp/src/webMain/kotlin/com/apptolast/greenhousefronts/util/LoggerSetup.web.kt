package com.apptolast.greenhousefronts.util

/** No Crashlytics on Web (JS/Wasm); Kermit's default console writer remains active. */
actual fun configureCrashlyticsLogging() = Unit
