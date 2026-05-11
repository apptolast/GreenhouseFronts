package com.apptolast.greenhousefronts.util

/** No Crashlytics on Desktop JVM; Kermit's default console writer remains active. */
actual fun configureCrashlyticsLogging() = Unit
