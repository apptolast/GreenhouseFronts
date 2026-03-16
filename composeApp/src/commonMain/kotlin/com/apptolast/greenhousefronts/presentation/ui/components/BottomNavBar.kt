package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    GREENHOUSES("Invernaderos", Icons.Filled.Yard, Icons.Outlined.Yard),
    ALERTS("Alertas", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    PROFILE("Perfil", Icons.Filled.Person, Icons.Outlined.Person),
}

@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        BottomNavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewBottomNavBar() {
    GreenhouseTheme(darkTheme = true) {
        BottomNavBar(
            selectedTab = BottomNavTab.GREENHOUSES,
            onTabSelected = {},
        )
    }
}
