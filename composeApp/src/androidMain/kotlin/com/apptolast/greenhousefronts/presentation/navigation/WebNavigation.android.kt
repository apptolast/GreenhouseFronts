package com.apptolast.greenhousefronts.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Android implementation of web navigation configuration.
 * This is a no-op as Android doesn't need browser integration.
 */
@Composable
actual fun ConfigureWebNavigation(navController: NavHostController) {
    // No-op on Android
}
