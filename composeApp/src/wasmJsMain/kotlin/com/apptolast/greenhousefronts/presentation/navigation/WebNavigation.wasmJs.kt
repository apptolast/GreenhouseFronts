package com.apptolast.greenhousefronts.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * WebAssembly (Wasm) implementation of web navigation configuration.
 * Note: bindToBrowserNavigation() is not yet available for wasmJs target.
 * This is a no-op until the feature is added in future Navigation versions.
 */
@Composable
actual fun ConfigureWebNavigation(navController: NavHostController) {
    // TODO: Enable browser navigation when bindToBrowserNavigation() is available for wasmJs
    // LaunchedEffect(navController) {
    //     navController.bindToBrowserNavigation()
    // }
}
