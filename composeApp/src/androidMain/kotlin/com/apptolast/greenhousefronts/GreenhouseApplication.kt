package com.apptolast.greenhousefronts

import android.app.Application
import com.apptolast.greenhousefronts.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * Android Application class for GreenhouseFronts
 * Initializes Koin dependency injection framework
 */
class GreenhouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            // Android-specific configuration
            androidLogger()  // Enable Android logging
            androidContext(this@GreenhouseApplication)  // Provide Android context
        }
    }
}
