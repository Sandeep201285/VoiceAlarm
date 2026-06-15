package com.echocare.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth     : Screen("auth")
    object Home     : Screen("home")
}


