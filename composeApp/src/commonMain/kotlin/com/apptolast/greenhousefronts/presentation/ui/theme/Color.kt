package com.apptolast.greenhousefronts.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Color palette for the Greenhouse application.
 *
 * The app primarily uses a dark theme with neon green accents,
 * inspired by greenhouse monitoring dashboard aesthetics.
 *
 * Light theme is provided as a fallback but the app is designed
 * primarily for dark mode usage.
 */

// ============================================================================
// Dark Theme Colors (Primary - extracted from design mockups)
// ============================================================================

// Primary green colors - Neon green for prominent actions and accents
private val GreenNeonPrimary = Color(0xFF00E676)        // Material Green A400 - Bright neon green
private val OnGreenNeonDark = Color(0xFF003300)         // Very dark green for text on neon
private val GreenDarkContainer = Color(0xFF1E3A34)      // Dark green-gray for containers
private val OnGreenDarkContainer = Color(0xFFB2DFDB)    // Light teal for text on dark containers

// Secondary green colors - Medium green for less prominent elements
private val GreenMediumSecondary = Color(0xFF4CAF50)    // Material Green 500
private val OnGreenMediumDark = Color(0xFF001A00)       // Almost black
private val GreenMediumContainer = Color(0xFF2E4A3E)    // Medium dark green-gray
private val OnGreenMediumContainer = Color(0xFFC8E6C9)  // Light green tint

// Tertiary colors - Teal accent for variety
private val TealAccent = Color(0xFF4ECDC4)              // Bright teal (used in humidity cards)
private val OnTealDark = Color(0xFF002020)              // Very dark teal
private val TealContainer = Color(0xFF1A3635)           // Dark teal-gray
private val OnTealContainer = Color(0xFFB2EBF2)         // Light cyan

// Surface and background colors - Very dark theme
private val BackgroundDark = Color(0xFF0F1419)          // Almost black with blue tint
private val SurfaceDark = Color(0xFF1A1E23)             // Dark gray for cards/surfaces
private val SurfaceVariantDark = Color(0xFF1E3A34)      // Dark green-tinted surface
private val OnBackgroundDark = Color(0xFFE6E1E5)        // Light gray for text
private val OnSurfaceDark = Color(0xFFE6E1E5)           // Light gray for text on surfaces
private val OnSurfaceVariantDark = Color(0xFFB0B0B0)    // Medium gray for secondary text

// Outline colors
private val OutlineDark = Color(0xFF2D5D4F)             // Medium green for borders
private val OutlineVariantDark = Color(0xFF1E3A34)      // Darker green for subtle borders

// Error colors (Material 3 defaults for dark theme)
private val ErrorDark = Color(0xFFF2B8B5)
private val OnErrorDark = Color(0xFF601410)
private val ErrorContainerDark = Color(0xFF8C1D18)
private val OnErrorContainerDark = Color(0xFFF9DEDC)

// Inverse colors for dark theme
private val InverseSurfaceDark = Color(0xFFE6E1E5)
private val InverseOnSurfaceDark = Color(0xFF1C1B1F)
private val InversePrimaryDark = Color(0xFF1B5E20)      // Dark green for inverse

// ============================================================================
// Light Theme Colors (Fallback - standard Material Design 3 green palette)
// ============================================================================

// Primary green colors - Dark saturated green for light theme
private val GreenDarkPrimary = Color(0xFF1B5E20)        // Material Green 900 - Dark saturated
private val OnGreenLight = Color(0xFFFFFFFF)            // White text on dark green
private val GreenLightContainer = Color(0xFFC8E6C9)     // Light green tint for containers
private val OnGreenLightContainer = Color(0xFF1B5E20)   // Dark green text on light containers

// Secondary green colors
private val GreenMediumSecondary2 = Color(0xFF388E3C)   // Material Green 700
private val OnGreenSecondaryLight = Color(0xFFFFFFFF)   // White text
private val GreenSecondaryContainer = Color(0xFFA5D6A7) // Light green
private val OnGreenSecondaryContainerLight = Color(0xFF1B5E20)

// Tertiary colors - Teal for light theme
private val TealLight = Color(0xFF00796B)               // Material Teal 700
private val OnTealLight = Color(0xFFFFFFFF)             // White text
private val TealLightContainer = Color(0xFFB2DFDB)      // Light teal
private val OnTealLightContainer = Color(0xFF004D40)    // Dark teal

// Surface and background colors - Light theme
private val BackgroundLight = Color(0xFFFAFAFA)         // Very light gray
private val SurfaceLight = Color(0xFFFFFFFF)            // White
private val SurfaceVariantLight = Color(0xFFF1F1F1)     // Light gray
private val OnBackgroundLight = Color(0xFF1C1B1F)       // Almost black
private val OnSurfaceLight = Color(0xFF1C1B1F)          // Almost black
private val OnSurfaceVariantLight = Color(0xFF49454F)   // Medium gray

// Outline colors for light theme
private val OutlineLight = Color(0xFF79747E)            // Medium gray
private val OutlineVariantLight = Color(0xFFCAC4D0)     // Light gray

// Error colors (Material 3 defaults for light theme)
private val ErrorLight = Color(0xFFB3261E)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFF9DEDC)
private val OnErrorContainerLight = Color(0xFF410E0B)

// Inverse colors for light theme
private val InverseSurfaceLight = Color(0xFF313033)
private val InverseOnSurfaceLight = Color(0xFFF4EFF4)
private val InversePrimaryLight = Color(0xFF81C784)     // Light green for inverse

// ============================================================================
// Color Schemes - Material Design 3 ColorScheme objects
// ============================================================================

/**
 * Dark color scheme for the Greenhouse app.
 * Features a dark background with neon green accents for a modern,
 * tech-focused aesthetic suitable for greenhouse monitoring.
 */
internal val DarkColorScheme = darkColorScheme(
    // Primary colors - Neon green for main actions and emphasis
    primary = GreenNeonPrimary,
    onPrimary = OnGreenNeonDark,
    primaryContainer = GreenDarkContainer,
    onPrimaryContainer = OnGreenDarkContainer,

    // Secondary colors - Medium green for less prominent elements
    secondary = GreenMediumSecondary,
    onSecondary = OnGreenMediumDark,
    secondaryContainer = GreenMediumContainer,
    onSecondaryContainer = OnGreenMediumContainer,

    // Tertiary colors - Teal accent for variety (humidity displays, etc.)
    tertiary = TealAccent,
    onTertiary = OnTealDark,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,

    // Error colors
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    // Background and surface colors
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    // Outline colors
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,

    // Inverse colors
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,

    // Scrim for overlays
    scrim = Color.Black
)

/**
 * Light color scheme for the Greenhouse app.
 * Provided as a fallback option, though the app is primarily designed for dark mode.
 */
internal val LightColorScheme = lightColorScheme(
    // Primary colors - Dark saturated green
    primary = GreenDarkPrimary,
    onPrimary = OnGreenLight,
    primaryContainer = GreenLightContainer,
    onPrimaryContainer = OnGreenLightContainer,

    // Secondary colors
    secondary = GreenMediumSecondary2,
    onSecondary = OnGreenSecondaryLight,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = OnGreenSecondaryContainerLight,

    // Tertiary colors - Teal
    tertiary = TealLight,
    onTertiary = OnTealLight,
    tertiaryContainer = TealLightContainer,
    onTertiaryContainer = OnTealLightContainer,

    // Error colors
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    // Background and surface colors
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    // Outline colors
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,

    // Inverse colors
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,

    // Scrim for overlays
    scrim = Color.Black
)
