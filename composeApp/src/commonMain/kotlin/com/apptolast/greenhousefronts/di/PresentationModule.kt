package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.SensorDetailViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Presentation layer module containing ViewModels
 */
val presentationModule = module {

    // ViewModels - special factory that respects lifecycle
    // Constructor injection: repository will be auto-injected
    viewModelOf(::AuthViewModel)
    viewModelOf(::GreenhouseViewModel)
    viewModelOf(::SensorDetailViewModel)
    viewModelOf(::SettingsViewModel)
}
