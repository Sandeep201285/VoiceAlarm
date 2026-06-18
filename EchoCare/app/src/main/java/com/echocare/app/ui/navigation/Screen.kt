package com.echocare.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding   : Screen("onboarding")
    object Auth         : Screen("auth")
    object MainContainer: Screen("main_container")
    
    // Nested tab destinations
    object HomeTab      : Screen("home_tab")
    object AlarmsTab    : Screen("alarms_tab")
    object SettingsTab  : Screen("settings_tab")
    object Groups       : Screen("groups")
}


