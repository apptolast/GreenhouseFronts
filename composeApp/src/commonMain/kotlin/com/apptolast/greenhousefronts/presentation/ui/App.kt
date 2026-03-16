package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.apptolast.greenhousefronts.presentation.navigation.ConfigureWebNavigation
import com.apptolast.greenhousefronts.presentation.navigation.ForgotPasswordRoute
import com.apptolast.greenhousefronts.presentation.navigation.HomeRoute
import com.apptolast.greenhousefronts.presentation.navigation.LoginRoute
import com.apptolast.greenhousefronts.presentation.navigation.RegisterRoute
import com.apptolast.greenhousefronts.presentation.navigation.ResetPasswordRoute
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

object StaticNavigator {
    lateinit var navController: NavHostController

    // Stores deeplinks received before NavController is ready
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

        // Configure platform-specific navigation (e.g., browser integration on Web)
        ConfigureWebNavigation(navController)

        NavHost(
            navController = navController,
            startDestination = LoginRoute,
        ) {
            // Login screen route
            composable<LoginRoute> {
                val authViewModel: AuthViewModel = koinViewModel()
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(ForgotPasswordRoute)
                    },
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
                        navController.navigate(ResetPasswordRoute(token = "")) {
                            popUpTo(LoginRoute)
                        }
                    },
                )
            }

            // Reset Password screen route (Deeplink)
            composable<ResetPasswordRoute>(
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern =
                            "http://localhost:8080/#com.apptolast.greenhousefronts.presentation.navigation.ResetPasswordRoute%2F{token}"
                    },
                    navDeepLink {
                        uriPattern = "http://apptolast.com/?token={token}"
                    },
                    navDeepLink {
                        uriPattern = "https://apptolast.com/?token={token}"
                    },
                    navDeepLink {
                        uriPattern = "greenhouse://reset?token={token}"
                    },
                ),
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
                        navController.navigate(LoginRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                )
            }

            // Register screen route
            composable<RegisterRoute> {
                val authViewModel: AuthViewModel = koinViewModel()
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                )
            }

            // Home screen route (placeholder)
            composable<HomeRoute> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
