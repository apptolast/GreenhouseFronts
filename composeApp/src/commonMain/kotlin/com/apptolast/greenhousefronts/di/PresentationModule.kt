package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf

/**
 * Presentation layer module containing ViewModels
 */
val presentationModule = module {

    // ViewModel - special factory that respects lifecycle
    // Constructor injection: repository will be auto-injected
    viewModelOf(::GreenhouseViewModel)
}
