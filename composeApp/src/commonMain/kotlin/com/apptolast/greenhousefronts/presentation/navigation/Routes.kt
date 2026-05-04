package com.apptolast.greenhousefronts.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Splash / route-decider. The single fixed `startDestination` of the only NavHost. It
 * runs `AuthRepository.bootstrap()` on entry and routes the user to either [LoginRoute] or
 * [GreenhousesRoute] based on the resulting [com.apptolast.greenhousefronts.domain.model.AuthState].
 *
 * Following the canonical Navigation Compose pattern (see Android Developers and Mantel's
 * conditional-navigation guide): keep one fixed start destination, decide the real
 * destination from a splash, then `popUpTo(SplashRoute) { inclusive = true }` so the
 * splash is removed from the back stack.
 */
@Serializable
object SplashRoute

/**
 * Represents the login screen route.
 * Shown after the splash decides there is no valid session.
 */
@Serializable
object LoginRoute

/**
 * Represents the registration screen route.
 * Allows new tenants/companies to create an account.
 */
@Serializable
object RegisterRoute

/**
 * Represents the forgot password screen route.
 * Allows users to request a password reset email.
 */
@Serializable
object ForgotPasswordRoute

/**
 * Represents the reset password screen route.
 * Triggered via deeplink with a token.
 *
 * @property token The secure token to verify identity
 */
@Serializable
data class ResetPasswordRoute(
    val token: String
)

/**
 * Represents the greenhouses screen route.
 * This is the main screen after successful login, showing the greenhouse list.
 */
@Serializable
object GreenhousesRoute

/**
 * Represents the greenhouse detail screen route.
 *
 * @property greenhouseId ID of the greenhouse to display
 */
@Serializable
data class GreenhouseDetailRoute(
    val greenhouseId: Long,
)

/**
 * Represents the irrigation configuration screen route.
 *
 * @property greenhouseId ID of the greenhouse to configure irrigation for
 */
@Serializable
data class IrrigationConfigRoute(
    val greenhouseId: Long,
)

/**
 * Represents the device detail screen route.
 *
 * @property deviceCode Device code (e.g., "DEV-00031")
 * @property greenhouseId Greenhouse ID for WebSocket context
 */
@Serializable
data class DeviceDetailRoute(
    val deviceCode: String,
    val greenhouseId: Long,
)

/**
 * Notification preferences screen — categories, severity threshold, quiet hours,
 * preferred channel, locale. Reached from Profile.
 */
@Serializable
object NotificationPreferencesRoute

/**
 * In-app notification log — paginated history of pushes delivered to the user.
 * Reached from Profile.
 */
@Serializable
object NotificationLogRoute
