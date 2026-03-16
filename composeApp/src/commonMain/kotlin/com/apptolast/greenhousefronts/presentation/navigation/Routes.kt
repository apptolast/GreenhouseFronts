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
 * Represents the greenhouses screen route.
 * This is the main screen after successful login, showing the greenhouse list.
 */
@Serializable
object GreenhousesRoute
