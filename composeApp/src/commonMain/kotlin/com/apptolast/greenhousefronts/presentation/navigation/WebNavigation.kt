package com.apptolast.greenhousefronts.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Platform-specific navigation configuration.
 * On Web platforms, this binds the navigation to browser history.
 * On other platforms, this is a no-op.
 */
@Composable
expect fun ConfigureWebNavigation(navController: NavHostController)
