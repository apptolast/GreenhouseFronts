package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.presentation.navigation.ConfigureWebNavigation
import com.apptolast.greenhousefronts.presentation.navigation.ForgotPasswordRoute
import com.apptolast.greenhousefronts.presentation.navigation.HomeRoute
import com.apptolast.greenhousefronts.presentation.navigation.LoginRoute
import com.apptolast.greenhousefronts.presentation.navigation.RegisterRoute
import com.apptolast.greenhousefronts.presentation.navigation.ResetPasswordRoute
import com.apptolast.greenhousefronts.presentation.navigation.SensorDetailRoute
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.SensorDetailViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

object StaticNavigator {
    lateinit var navController: NavHostController

    // Almacena deeplinks recibidos antes de que el NavController esté listo
    var pendingResetToken: String? = null

    fun isReady(): Boolean = ::navController.isInitialized
}

/**
 * Main application composable that sets up the navigation graph.
 * Uses androidx.navigation.compose for type-safe navigation between screens.
 * Wrapped with GreenhouseTheme for custom Material Design 3 theming.
 */
@Composable
@Preview
fun App() {
    GreenhouseTheme(darkTheme = true) {
        val navController = rememberNavController()
        StaticNavigator.navController = navController

        // Used for global snackbars if needed, or we can pass a host state
        val scope = rememberCoroutineScope()
        // Note: Simple snackbar for global feedback if screens don't handle it

        // Configure platform-specific navigation (e.g., browser integration on Web)
        ConfigureWebNavigation(navController)

        NavHost(
            navController = navController,
            startDestination = LoginRoute
        ) {
            // Login screen route
            composable<LoginRoute> {
                val authViewModel: AuthViewModel = koinViewModel()
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(HomeRoute) {
                            // Clear auth screens from back stack
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(ForgotPasswordRoute)
                    }
                )
            }

            // Forgot Password screen route
            composable<ForgotPasswordRoute> {
                val authViewModel: AuthViewModel = koinViewModel()
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onNavigateBack = {
                        authViewModel.clearForgotPasswordForm()
                        navController.popBackStack()
                    },
                    onSuccess = {
                        authViewModel.clearForgotPasswordForm()
                        // Navigate to ResetPasswordScreen with test token for development
                        // TODO: Replace with actual token from backend response
                        navController.navigate(ResetPasswordRoute(token = "")) {
                            // Remove ForgotPasswordScreen from back stack
                            popUpTo(LoginRoute)
                        }
                    }
                )
            }

            // Reset Password screen route (Deeplink)
            composable<ResetPasswordRoute>(
                deepLinks = listOf(
                    // Web local - URL-encoded path (/ = %2F)
                    // Use: http://localhost:8080/#ResetPasswordRoute%2F<token>
                    navDeepLink {
                        uriPattern =
                            "http://localhost:8080/#com.apptolast.greenhousefronts.presentation.navigation.ResetPasswordRoute%2F{token}"

                    },
                    // Mobile deep links
                    navDeepLink {
                        uriPattern = "http://apptolast.com/?token={token}"
                    },
                    navDeepLink {
                        uriPattern = "https://apptolast.com/?token={token}"
                    },
                    // Mobile custom scheme
                    navDeepLink {
                        uriPattern = "greenhouse://reset?token={token}"
                    }
                )
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<ResetPasswordRoute>()
                val authViewModel: AuthViewModel = koinViewModel()

                ResetPasswordScreen(
                    token = route.token,
                    viewModel = authViewModel,
                    onNavigateBack = {
                        authViewModel.clearResetPasswordForm()
                        navController.popBackStack()
                    },
                    onSuccess = {
                        authViewModel.clearResetPasswordForm()
                        // Navigate to login and clear back stack
                        navController.navigate(LoginRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }

            // Register screen route
            composable<RegisterRoute> {
                val authViewModel: AuthViewModel = koinViewModel()
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(HomeRoute) {
                            // Clear auth screens from back stack
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // Home screen route
            composable<HomeRoute> {
                // koinViewModel() automatically handles Navigation integration in Koin 4.1+
                // It provides NavBackStackEntry integration and SavedStateHandle support
                val viewModel: GreenhouseViewModel = koinViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSensorDetail = { greenhouseId, sensorType ->
                        navController.navigate(
                            SensorDetailRoute(
                                greenhouseId = greenhouseId,
                                sensorType = sensorType
                            )
                        )
                    }
                )
            }

            // Sensor detail screen route
            composable<SensorDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<SensorDetailRoute>()
                val sensorType = SensorType.fromApiValue(route.sensorType) ?: SensorType.TEMPERATURE
                val viewModel: SensorDetailViewModel = koinViewModel()

                SensorDetailScreen(
                    greenhouseId = route.greenhouseId,
                    sensorType = sensorType,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

