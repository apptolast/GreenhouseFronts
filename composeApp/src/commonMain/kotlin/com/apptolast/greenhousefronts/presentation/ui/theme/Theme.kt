package com.apptolast.greenhousefronts.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Main theme composable for the Greenhouse application.
 *
 * This theme wraps the entire app and provides:
 * - Material Design 3 color system with custom dark/light schemes
 * - Typography definitions following Material 3 type scale
 * - Automatic dark/light theme switching based on system settings
 *
 * The app is primarily designed for dark mode with neon green accents,
 * providing a modern, tech-focused aesthetic suitable for greenhouse monitoring.
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
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
