package com.apptolast.greenhousefronts.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavHostController
import androidx.navigation.bindToBrowserNavigation

/**
 * JavaScript implementation of web navigation configuration.
 * Note: bindToBrowserNavigation() is not available in Navigation 2.9.1 for JS target.
 * This functionality may be added in future Navigation releases (possibly Navigation 3.0).
 * For now, this is a no-op.
 */
@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
actual fun ConfigureWebNavigation(navController: NavHostController) {
    // TODO: Enable browser navigation when bindToBrowserNavigation() is available for JS
    LaunchedEffect(navController) {
        navController.bindToBrowserNavigation()
    }
}
