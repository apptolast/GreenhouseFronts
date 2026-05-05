package com.apptolast.greenhousefronts.presentation.ui.theme

import androidx.compose.runtime.Composable

/**
 * iOS-specific implementation of system UI configuration.
 *
 * Configures status bar appearance (light/dark content) for iOS.
 *
 * TODO: Implement UIKit status bar configuration for iOS
 * - Use UIApplication.shared.statusBarStyle
 * - Configure for light content in dark mode, dark content in light mode
 *
 * This is called automatically by GreenhouseTheme.
 *
 * @param darkTheme Whether dark theme is active
 */
@Composable
actual fun ConfigureSystemUI(darkTheme: Boolean) {
    // TODO: Implement iOS status bar configuration
    // For now, iOS will use default status bar appearance
}