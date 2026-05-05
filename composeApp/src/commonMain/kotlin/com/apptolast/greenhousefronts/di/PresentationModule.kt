package com.apptolast.greenhousefronts.di

import com.apptolast.greenhousefronts.presentation.viewmodel.AlertsViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.DeviceDetailViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseListViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.IrrigationConfigViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.ProfileViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.SendSuggestionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Presentation layer module containing ViewModels
 */
val presentationModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::GreenhouseListViewModel)
    viewModelOf(::DeviceDetailViewModel)
    viewModelOf(::GreenhouseDetailViewModel)
    viewModelOf(::IrrigationConfigViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::AlertsViewModel)
    viewModelOf(::SendSuggestionViewModel)
}
