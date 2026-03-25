package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Fixed-height loading bar that reserves its space in the layout
 * to prevent content shifting when toggling visibility.
 */
@Composable
fun LoadingBar(isLoading: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
