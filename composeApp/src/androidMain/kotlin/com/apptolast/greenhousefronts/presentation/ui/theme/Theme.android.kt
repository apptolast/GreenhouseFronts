package com.apptolast.greenhousefronts.presentation.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Android-specific implementation of system UI configuration.
 *
 * Configures status bar and navigation bar icon appearance:
 * - **Dark mode**: Light (white) icons for visibility on dark backgrounds
 * - **Light mode**: Dark icons for visibility on light backgrounds
 *
 * Uses WindowCompat.getInsetsController() which works across all API levels
 * without requiring manual API version checks.
 *
 * This is called automatically by GreenhouseTheme.
 *
 * @param darkTheme Whether dark theme is active
 */
@Composable
actual fun ConfigureSystemUI(darkTheme: Boolean) {
    val view = LocalView.current

    // Configure status bar and navigation bar icon colors
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Set light or dark icons based on theme
            // isAppearanceLightStatusBars = true means "use dark icons" (for light backgrounds)
            // isAppearanceLightStatusBars = false means "use light icons" (for dark backgrounds)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}