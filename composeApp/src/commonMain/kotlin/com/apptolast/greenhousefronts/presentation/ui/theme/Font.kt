package com.apptolast.greenhousefronts.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Custom font family definitions for the Greenhouse application.
 *
 * ## Current Font
 * Currently using the default system font (FontFamily.Default).
 *
 * ## How to Add Custom Fonts
 *
 * ### Step 1: Download Font Files
 * - Visit Google Fonts: https://fonts.google.com
 * - Recommended: Inter (https://fonts.google.com/specimen/Inter)
 * - Download at least 4 weights: Regular (400), Medium (500), SemiBold (600), Bold (700)
 *
 * ### Step 2: Place Font Files
 * 1. Create directory: `composeApp/src/commonMain/composeResources/font/`
 * 2. Copy .ttf files with lowercase names:
 *    - inter_regular.ttf
 *    - inter_medium.ttf
 *    - inter_semibold.ttf
 *    - inter_bold.ttf
 *
 * ### Step 3: Build Project
 * Run `./gradlew build` to generate resource accessors
 *
 * ### Step 4: Update This File
 * Uncomment and use the custom font code below:
 *
 * ```kotlin
 * import org.jetbrains.compose.resources.Font
 * import greenhousefronts.composeapp.generated.resources.Res
 * import greenhousefronts.composeapp.generated.resources.inter_regular
 * import greenhousefronts.composeapp.generated.resources.inter_medium
 * import greenhousefronts.composeapp.generated.resources.inter_semibold
 * import greenhousefronts.composeapp.generated.resources.inter_bold
 *
 * @Composable
 * fun appFontFamily(): FontFamily {
 *     return FontFamily(
 *         Font(Res.font.inter_regular, FontWeight.Normal),
 *         Font(Res.font.inter_medium, FontWeight.Medium),
 *         Font(Res.font.inter_semibold, FontWeight.SemiBold),
 *         Font(Res.font.inter_bold, FontWeight.Bold)
 *     )
 * }
 * ```
 *
 * ## Swapping Fonts Later
 *
 * To change to a different font (e.g., Roboto):
 * 1. Download new font files from Google Fonts
 * 2. Place in `composeResources/font/` with descriptive names
 * 3. Update the imports and Font() calls in this file
 * 4. Rebuild the project
 * 5. No other files need changes - Type.kt references this function automatically
 *
 * ## Recommended Fonts for Tech/Dashboard Apps
 *
 * - **Inter**: Screen-optimized, excellent for dashboards (RECOMMENDED)
 * - **Roboto**: Android default, great for data/tables
 * - **Geist Sans**: Modern, developer-focused aesthetic
 * - **Manrope**: Space-efficient, good for dense information
 * - **DM Sans**: Optimized for small text sizes
 */

/**
 * Returns the font family used throughout the application.
 *
 * IMPORTANT: Font() in Compose Multiplatform is a @Composable function,
 * so this must be called from within a @Composable context.
 *
 * Currently using system default font. To use a custom font,
 * follow the instructions in the file header.
 *
 * @return FontFamily to be used in Typography definitions
 */
@Composable
fun appFontFamily(): FontFamily {
    // TODO: Replace with custom font after adding font files to composeResources/font/
    // For now, using default system font for all platforms
    return FontFamily.Default

    // Uncomment below after adding custom fonts:
    /*
    return FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold)
    )
    */
}

/**
 * Alternative font families for specific use cases.
 * Can be used for special sections or fallbacks.
 */

/**
 * Monospace font family for code snippets or technical data.
 * Uses system monospace font by default.
 */
@Composable
fun monospaceFontFamily(): FontFamily {
    return FontFamily.Monospace
}

/**
 * Sans-serif font family fallback.
 * Uses system sans-serif font.
 */
@Composable
fun sansSerifFontFamily(): FontFamily {
    return FontFamily.SansSerif
}
