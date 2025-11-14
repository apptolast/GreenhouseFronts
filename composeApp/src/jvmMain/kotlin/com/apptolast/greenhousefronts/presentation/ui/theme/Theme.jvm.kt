package com.apptolast.greenhousefronts.presentation.ui.theme

import androidx.compose.runtime.Composable

/**
 * Desktop (JVM) implementation of system UI configuration.
 *
 * Desktop applications don't have a system status bar in the same way
 * mobile platforms do, so this is a no-op implementation.
 *
 * This is called automatically by GreenhouseTheme.
 *
 * @param darkTheme Whether dark theme is active
 */
@Composable
actual fun ConfigureSystemUI(darkTheme: Boolean) {
    // Desktop doesn't have a system status bar to configure
    // No-op implementation
}