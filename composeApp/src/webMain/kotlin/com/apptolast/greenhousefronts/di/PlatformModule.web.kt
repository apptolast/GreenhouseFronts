package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.feedback.MailLauncher
import com.apptolast.greenhousefronts.data.feedback.WebMailLauncher
import org.koin.dsl.module

/**
 * Web-specific dependencies (JS and Wasm)
 */
actual fun platformModule() = module {
    single<MailLauncher> { WebMailLauncher() }
}
