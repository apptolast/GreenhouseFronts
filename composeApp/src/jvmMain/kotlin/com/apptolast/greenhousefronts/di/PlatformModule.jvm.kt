package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.feedback.JvmMailLauncher
import com.apptolast.greenhousefronts.data.feedback.MailLauncher
import org.koin.dsl.module

/**
 * Desktop (JVM) specific dependencies
 */
actual fun platformModule() = module {
    single<MailLauncher> { JvmMailLauncher() }
}
