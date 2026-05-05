package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    alertsBadgeCount: Int = 0,
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
                    val iconImage = if (isSelected) tab.selectedIcon else tab.unselectedIcon
                    if (tab == BottomNavTab.ALERTS && alertsBadgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    modifier = Modifier.offset(x = 5.dp, y = (-5).dp)
                                ) {
                                    // Cap to 99+ to keep the bubble compact when the
                                    // count gets large; M3 Badge auto-sizes to content.
                                    val display = if (alertsBadgeCount > 99) "99+" else alertsBadgeCount.toString()
                                    Text(text = display)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = iconImage,
                                contentDescription = tab.label,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = iconImage,
                            contentDescription = tab.label,
                        )
                    }
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

@Preview
@Composable
private fun PreviewBottomNavBarWithBadge() {
    GreenhouseTheme(darkTheme = true) {
        BottomNavBar(
            selectedTab = BottomNavTab.ALERTS,
            onTabSelected = {},
            alertsBadgeCount = 7,
        )
    }
}
