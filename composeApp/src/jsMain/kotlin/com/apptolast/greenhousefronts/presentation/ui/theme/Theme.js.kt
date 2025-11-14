package com.apptolast.greenhousefronts.presentation.ui.theme

import androidx.compose.runtime.Composable

/**
 * Web (JS) implementation of system UI configuration.
 *
 * Web applications don't have a native system status bar to configure,
 * so this is a no-op implementation.
 *
 * This is called automatically by GreenhouseTheme.
 *
 * @param darkTheme Whether dark theme is active
 */
@Composable
actual fun ConfigureSystemUI(darkTheme: Boolean) {
    // Web doesn't have a system status bar to configure
    // No-op implementation
}