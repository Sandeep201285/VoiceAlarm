package com.echocare.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.echocare.app.ui.screens.home.HomeScreen
import com.echocare.app.ui.screens.home.AlarmsScreen
import com.echocare.app.ui.screens.settings.SettingsScreen
import com.echocare.app.ui.screens.groups.GroupsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer() {
    val tabNavController = rememberNavController()
    
    // Bottom Nav Items
    val items = listOf(
        NavigationItem("Home", Screen.HomeTab.route, Icons.Default.Home),
        NavigationItem("Alarms", Screen.AlarmsTab.route, Icons.Default.AccessTime),
        NavigationItem("Settings", Screen.SettingsTab.route, Icons.Default.Settings)
    )

    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                tabNavController.navigate(item.route) {
                                    popUpTo(Screen.HomeTab.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = tabNavController,
            startDestination = Screen.HomeTab.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.HomeTab.route) {
                HomeScreen(
                    onMyAlarmsClick = {
                        tabNavController.navigate(Screen.AlarmsTab.route) {
                            popUpTo(Screen.HomeTab.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGroupsClick = {
                        tabNavController.navigate(Screen.Groups.route)
                    }
                )
            }
            
            composable(Screen.AlarmsTab.route) {
                AlarmsScreen()
            }
            
            composable(Screen.SettingsTab.route) {
                SettingsScreen()
            }
            
            composable(Screen.Groups.route) {
                GroupsScreen(
                    onBackClick = {
                        tabNavController.popBackStack()
                    }
                )
            }
        }
    }
}

private data class NavigationItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
