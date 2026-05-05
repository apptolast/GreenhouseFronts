package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.data.feedback.AndroidMailLauncher
import com.apptolast.greenhousefronts.data.feedback.MailLauncher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific dependencies
 */
actual fun platformModule() = module {
    single<MailLauncher> { AndroidMailLauncher(androidContext()) }
}
