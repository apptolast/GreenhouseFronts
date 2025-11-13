package com.apptolast.greenhousefronts.di

import org.koin.dsl.module

/**
 * Domain layer module containing use cases and business logic
 * Currently empty as repository is injected directly into ViewModel
 * Add use cases here as the app grows
 */
val domainModule = module {
    // Future use cases will go here
    // Example:
    // factory { GetRecentMessagesUseCase(get()) }
}
