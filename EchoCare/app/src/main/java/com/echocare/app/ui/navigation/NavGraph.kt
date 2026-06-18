package com.echocare.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.echocare.app.ui.screens.auth.AuthScreen
import com.echocare.app.ui.screens.auth.AuthViewModel
import com.echocare.app.ui.screens.home.HomeScreen
import com.echocare.app.ui.screens.add.AddReminderScreen
import com.echocare.app.ui.screens.settings.SettingsScreen
import com.echocare.app.ui.screens.groups.GroupViewModel
import com.echocare.app.ui.screens.groups.GroupListScreen
import com.echocare.app.ui.screens.groups.CreateGroupScreen
import com.echocare.app.ui.screens.groups.GroupMembersScreen
import com.echocare.app.ui.screens.onboarding.OnboardingScreen
import com.echocare.app.ui.screens.auth.PaywallScreen
import com.echocare.app.ui.screens.add.TemplateListScreen
import com.echocare.app.ui.screens.home.HomeViewModel

@Composable
fun NavGraph(authViewModel: AuthViewModel) {
    val navController   = rememberNavController()
    val isOnboarded     by authViewModel.isOnboarded.collectAsState()
    val isLoading       by authViewModel.isLoading.collectAsState()
    val homeViewModel: HomeViewModel = hiltViewModel()

    if (isLoading) return

    val startDestination = if (isOnboarded) Screen.MainContainer.route else Screen.Onboarding.route

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {
        // ─── Auth / Onboarding ───────────────────────────────────────
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthComplete = {
                    navController.navigate(Screen.MainContainer.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // ─── Main Scaffold ───────────────────────────────────────────
        composable(Screen.MainContainer.route) {
            MainContainer()
        }
    }
}
