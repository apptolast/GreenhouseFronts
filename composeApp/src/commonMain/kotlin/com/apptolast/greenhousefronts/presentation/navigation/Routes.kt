package com.apptolast.greenhousefronts.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Represents the login screen route.
 * This is the initial screen where users authenticate.
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
 * Represents the home screen route.
 * This is the main screen after successful login, showing greenhouse data.
 */
@Serializable
object HomeRoute

/**
 * Represents the sensor detail screen route.
 * Shows historical data and statistics for a specific sensor in a greenhouse.
 *
 * @property greenhouseId UUID of the greenhouse
 * @property sensorType Type of sensor (e.g., "TEMPERATURE", "HUMIDITY")
 */
@Serializable
data class SensorDetailRoute(
    val greenhouseId: String,
    val sensorType: String
)

/**
 * Represents the settings screen route.
 * Provides access to app configuration and logout functionality.
 */
@Serializable
object SettingsRoute
