package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.apptolast.greenhousefronts.data.remote.push.AlertDeepLinkBus
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.presentation.navigation.BottomNavSelectionBus
import com.apptolast.greenhousefronts.presentation.navigation.ConfigureWebNavigation
import com.apptolast.greenhousefronts.presentation.navigation.PendingAlertSelectionBus
import com.apptolast.greenhousefronts.presentation.ui.components.BottomNavTab
import com.apptolast.greenhousefronts.presentation.navigation.ForgotPasswordRoute
import com.apptolast.greenhousefronts.presentation.navigation.GreenhouseDetailRoute
import com.apptolast.greenhousefronts.presentation.navigation.GreenhousesRoute
import com.apptolast.greenhousefronts.presentation.navigation.DeviceDetailRoute
import com.apptolast.greenhousefronts.presentation.navigation.IrrigationConfigRoute
import com.apptolast.greenhousefronts.presentation.navigation.LoginRoute
import com.apptolast.greenhousefronts.presentation.navigation.RegisterRoute
import com.apptolast.greenhousefronts.presentation.navigation.ResetPasswordRoute
import com.apptolast.greenhousefronts.presentation.navigation.SplashRoute
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.AuthViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.DeviceDetailViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.GreenhouseDetailViewModel
import com.apptolast.greenhousefronts.presentation.viewmodel.IrrigationConfigViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
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
 *
 * The single fixed `startDestination` is [SplashRoute]; the splash itself reads the
 * cached session via `AuthRepository.bootstrap()` and routes to either Login or the main
 * graph, then pops itself off the back stack. This is the canonical Navigation Compose
 * pattern for conditional auth routing.
 */
@Composable
@Preview
fun App() {
    GreenhouseTheme(darkTheme = true) {
        val navController = rememberNavController()
        StaticNavigator.navController = navController

        // Configure platform-specific navigation (e.g., browser integration on Web)
        ConfigureWebNavigation(navController)

        val authRepository: AuthRepository = koinInject()

        // Alert deep-link from FCM tap. The PendingAlertSelectionBus and
        // BottomNavSelectionBus are both StateFlow-backed, so they survive a Login
        // round-trip — when the user is logged-out at FCM time, we just stash the
        // selection and let LoginScreen.onLoginSuccess navigate to GreenhousesRoute,
        // where MainScreen replays the bus and lands on the Alerts tab automatically.
        val pendingAlertSelection: PendingAlertSelectionBus = koinInject()
        val bottomNavSelection: BottomNavSelectionBus = koinInject()
        LaunchedEffect(Unit) {
            AlertDeepLinkBus.events.collect { link ->
                pendingAlertSelection.set(link.alertId)
                bottomNavSelection.select(BottomNavTab.ALERTS)
                if (authRepository.authState.value is AuthState.Authenticated) {
                    navController.navigate(GreenhousesRoute) {
                        popUpTo(GreenhousesRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }

        // Global session feedback. EXPIRED also pops the back stack so protected screens
        // can't be revisited with the back button while unauthenticated.
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            authRepository.sessionEvents.collect { event ->
                if (event.reason == AuthState.Reason.EXPIRED) {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                snackbarHostState.showSnackbar(event.message)
            }
        }

        // Plain Box (NOT Scaffold) so we don't reserve window insets at this level — every
        // destination already has its own Scaffold (LoginScreen, MainScreen, the *Detail
        // screens, …) and would double-pad the status bar otherwise. The SnackbarHost is
        // anchored at the bottom and respects only the navigation-bar inset so it doesn't
        // get clipped by the gesture area.
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = SplashRoute,
            ) {
                // Splash route — decides where to go based on AuthState.
                composable<SplashRoute> {
                    SplashScreen(
                        authRepository = authRepository,
                        onAuthenticated = {
                            navController.navigate(GreenhousesRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        },
                        onUnauthenticated = {
                            navController.navigate(LoginRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        },
                    )
                }

                // Login screen route
                composable<LoginRoute> {
                    val authViewModel: AuthViewModel = koinViewModel()
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            navController.navigate(GreenhousesRoute) {
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
                            navController.navigate(GreenhousesRoute) {
                                popUpTo(LoginRoute) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        },
                    )
                }

                // Main screen (post-login) with bottom navigation
                composable<GreenhousesRoute> {
                    MainScreen(
                        onLogoutSuccess = {
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToGreenhouseDetail = { greenhouseId ->
                            navController.navigate(GreenhouseDetailRoute(greenhouseId))
                        },
                    )
                }

                // Greenhouse detail screen
                composable<GreenhouseDetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<GreenhouseDetailRoute>()
                    val viewModel: GreenhouseDetailViewModel = koinViewModel()
                    GreenhouseDetailScreen(
                        greenhouseId = route.greenhouseId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToIrrigationConfig = { greenhouseId ->
                            navController.navigate(IrrigationConfigRoute(greenhouseId))
                        },
                        onNavigateToDeviceDetail = { deviceCode ->
                            navController.navigate(DeviceDetailRoute(deviceCode, route.greenhouseId))
                        },
                        onNavigateToAlerts = {
                            navController.popBackStack()
                            bottomNavSelection.select(BottomNavTab.ALERTS)
                        },
                    )
                }

                // Irrigation configuration screen
                composable<IrrigationConfigRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<IrrigationConfigRoute>()
                    val viewModel: IrrigationConfigViewModel = koinViewModel()
                    IrrigationConfigScreen(
                        greenhouseId = route.greenhouseId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }

                // Device detail screen
                composable<DeviceDetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<DeviceDetailRoute>()
                    val viewModel: DeviceDetailViewModel = koinViewModel()
                    DeviceDetailScreen(
                        deviceCode = route.deviceCode,
                        greenhouseId = route.greenhouseId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }

            // Global Snackbar overlay. Anchored bottom-center, padded above the system
            // navigation bar so it doesn't overlap the gesture area, then a small extra
            // padding for breathing room. Z-order: rendered after NavHost so it floats
            // above all destinations.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
            )
        }
    }
}
