package com.apptolast.greenhousefronts.util

/**
 * Wires the global Kermit Logger to its platform-specific sinks. On Android and iOS this
 * adds a `CrashlyticsLogWriter` so that:
 *
 *  - `Logger.i { … }` and above are forwarded to Firebase Crashlytics as breadcrumbs
 *    (`Crashlytics.log(message)`) — these show up in the "Logs" tab of the crash report.
 *  - `Logger.e(throwable) { … }` is reported as a non-fatal (`Crashlytics.recordException`).
 *
 * On JVM and Web targets there is no Crashlytics SDK to bind to, so the actual is a no-op
 * and Kermit falls back to the default platform console writer.
 */
expect fun configureCrashlyticsLogging()
