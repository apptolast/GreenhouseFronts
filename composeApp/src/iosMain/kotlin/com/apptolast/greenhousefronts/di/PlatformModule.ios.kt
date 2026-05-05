package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.feedback.IosMailLauncher
import com.apptolast.greenhousefronts.data.feedback.MailLauncher
import org.koin.dsl.module

/**
 * iOS-specific dependencies
 */
actual fun platformModule() = module {
    single<MailLauncher> { IosMailLauncher() }
}
