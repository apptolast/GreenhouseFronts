package com.apptolast.greenhousefronts.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavHostController
import androidx.navigation.bindToBrowserNavigation

/**
 * WebAssembly (Wasm) implementation of web navigation configuration.
 * Enables browser navigation for wasmJs using bindToBrowserNavigation().
 */
@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
actual fun ConfigureWebNavigation(navController: NavHostController) {
    LaunchedEffect(navController) {
        navController.bindToBrowserNavigation()
    }
}
