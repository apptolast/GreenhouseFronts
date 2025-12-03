package com.apptolast.greenhousefronts.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Platform-specific system UI configuration (status bar, navigation bar).
 *
 * This is an expect/actual function that allows platform-specific implementations:
 * - **Android**: Configures status bar and navigation bar icon colors using WindowCompat
 * - **iOS**: Configures status bar appearance (light/dark content)
 * - **Desktop/Web**: No-op implementation (not applicable)
 *
 * This function is called internally by GreenhouseTheme and doesn't need to be
 * called directly by user code.
 *
 * @param darkTheme Whether dark theme is active
 */
@Composable
expect fun ConfigureSystemUI(darkTheme: Boolean)

/**
 * Main theme composable for the Greenhouse application.
 *
 * This theme wraps the entire app and provides:
 * - Material Design 3 color system with custom dark/light schemes
 * - Typography definitions following Material 3 type scale
 * - Automatic dark/light theme switching based on system settings
 * - Platform-specific system UI configuration (status bar, navigation bar)
 *
 * The app is primarily designed for dark mode with neon green accents,
 * providing a modern, tech-focused aesthetic suitable for greenhouse monitoring.
 *
 * **Status Bar Behavior:**
 * - In dark mode: Light (white) icons on transparent background
 * - In light mode: Dark icons on transparent background
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun App() {
 *     GreenhouseTheme {
 *         // Your app content here
 *     }
 * }
 * ```
 *
 * @param darkTheme Whether to use dark theme. Defaults to system dark mode setting.
 *                  On Android and iOS, this automatically detects the system preference.
 *                  On Desktop and Web, it detects at startup (doesn't update dynamically).
 * @param content The composable content to be themed.
 */
@Composable
fun GreenhouseTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Configure platform-specific system UI (status bar, navigation bar)
    ConfigureSystemUI(darkTheme)

    // Select color scheme based on theme preference
    // Dark theme is the primary design, light theme is a fallback
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content
    )
}
