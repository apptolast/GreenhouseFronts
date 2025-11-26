package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.apptolast.greenhousefronts.data.model.SensorType
import com.apptolast.greenhousefronts.presentation.navigation.ConfigureWebNavigation
import com.apptolast.greenhousefronts.presentation.navigation.HomeRoute
import com.apptolast.greenhousefronts.presentation.navigation.LoginRoute
import com.apptolast.greenhousefronts.presentation.navigation.RegisterRoute
import com.apptolast.greenhousefronts.presentation.navigation.SensorDetailRoute
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.SensorDetailViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

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

