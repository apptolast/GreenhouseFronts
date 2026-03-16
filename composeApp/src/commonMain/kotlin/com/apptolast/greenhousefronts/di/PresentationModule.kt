package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Presentation layer module containing ViewModels
 */
val presentationModule = module {
    viewModelOf(::AuthViewModel)
}
