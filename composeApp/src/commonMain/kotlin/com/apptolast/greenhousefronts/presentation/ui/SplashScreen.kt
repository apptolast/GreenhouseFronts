package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.ic_launcher_foreground
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Route-decider screen. Runs `AuthRepository.bootstrap()` once, observes [AuthState], and
 * triggers navigation to the right destination via [onAuthenticated] / [onUnauthenticated]
 * callbacks (so this composable stays free of `NavController` references).
 *
 * The Splash itself is removed from the back stack by the caller via
 * `popUpTo(SplashRoute) { inclusive = true }` — see `App.kt`.
 *
 * Visually all three [AuthState] cases render the same content (logo + spinner): the
 * `Authenticated` and `Unauthenticated` cases only stay on screen for a single frame
 * before the navigation `LaunchedEffect` fires. The reusable visual is extracted as
 * [SplashContent] so it can be previewed and reused in tests.
 */
@Composable
fun SplashScreen(
    authRepository: AuthRepository,
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
) {
    val state by authRepository.authState.collectAsState()

    LaunchedEffect(Unit) {
        authRepository.bootstrap()
    }

    LaunchedEffect(state) {
        when (state) {
            is AuthState.Authenticated -> onAuthenticated()
            is AuthState.Unauthenticated -> onUnauthenticated()
            AuthState.Loading -> Unit
        }
    }

    SplashContent()
}

/**
 * Stateless splash visual: app logo, name and a centered spinner. Identical for every
 * [AuthState] case — the navigation decision is not surfaced visually because all
 * non-Loading states are transient (one frame).
 */
@Composable
private fun SplashContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = "Kropia",
                modifier = Modifier.size(220.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kropia",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

// --- Previews ---

/**
 * Default splash state. Identical visual for [AuthState.Loading] (the canonical case
 * the user sees during bootstrap), [AuthState.Authenticated] (one-frame transient) and
 * [AuthState.Unauthenticated] (one-frame transient).
 */
@Preview
@Composable
private fun PreviewSplashContent() {
    GreenhouseTheme(darkTheme = true) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            SplashContent()
        }
    }
}

/**
 * Light-theme variant — currently the app boots with `darkTheme = true` (see
 * `App.kt:51`), but this preview is kept so that a future theme toggle is covered.
 */
@Preview
@Composable
private fun PreviewSplashContentLight() {
    GreenhouseTheme(darkTheme = false) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            SplashContent()
        }
    }
}
