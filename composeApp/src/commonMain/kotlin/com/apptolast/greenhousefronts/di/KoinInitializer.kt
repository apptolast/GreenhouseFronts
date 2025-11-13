package com.apptolast.greenhousefronts.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin dependency injection framework
 * Call this function from each platform's entry point
 *
 * @param appDeclaration Optional configuration for platform-specific setup
 */
fun initKoin(appDeclaration: KoinAppDeclaration? = null) {
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
