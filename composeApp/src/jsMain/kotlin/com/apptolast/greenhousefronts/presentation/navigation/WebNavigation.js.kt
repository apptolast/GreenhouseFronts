package com.apptolast.greenhousefronts.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavHostController
import androidx.navigation.bindToBrowserNavigation

/**
 * JavaScript implementation of web navigation configuration.
 * Enables browser navigation for JS using bindToBrowserNavigation().
 */
@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
actual fun ConfigureWebNavigation(navController: NavHostController) {
    LaunchedEffect(navController) {
        navController.bindToBrowserNavigation()
    }
}
