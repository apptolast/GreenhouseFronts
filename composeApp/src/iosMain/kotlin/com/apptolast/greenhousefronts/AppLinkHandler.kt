package com.apptolast.greenhousefronts

import com.apptolast.greenhousefronts.presentation.navigation.ResetPasswordRoute
import com.apptolast.greenhousefronts.presentation.ui.StaticNavigator

/**
 * Handle Reset Password from iOS applink
 */
fun HandleResetPassword(token: String) {
    if (StaticNavigator.isReady()) {
        StaticNavigator.navController.navigate(ResetPasswordRoute(token))
    } else {
        // Guardar para consumir cuando el NavController se inicialice
        StaticNavigator.pendingResetToken = token
    }
}
