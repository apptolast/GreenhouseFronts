package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.util.configureCrashlyticsLogging
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin dependency injection framework
 * Call this function from each platform's entry point
 *
 * @param appDeclaration Optional configuration for platform-specific setup
 */
fun initKoin(appDeclaration: KoinAppDeclaration? = null) {
    // Kermit + Crashlytics: wire log forwarding BEFORE anything is logged so we don't miss
    // breadcrumbs emitted during Koin start-up.
    configureCrashlyticsLogging()

    startKoin {
        // Platform-specific configuration (optional)
        appDeclaration?.invoke(this)

        // Load all modules
        modules(
            dataModule,
            domainModule,
            presentationModule,
            platformModule()
        )
    }
}
