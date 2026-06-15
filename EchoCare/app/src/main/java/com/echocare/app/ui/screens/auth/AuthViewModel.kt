package com.echocare.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echocare.app.util.DataStoreHelper
import com.echocare.app.util.EchoCareTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dataStore: DataStoreHelper
) : ViewModel() {

    val isUnlocked = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = MutableStateFlow(true).also {
        // Let the splash screen stay until we know the onboarding state
        viewModelScope.launch {
            dataStore.isOnboarded.collect { _ ->
                (it as MutableStateFlow).value = false
            }
        }
    }

    val isOnboarded: StateFlow<Boolean> = dataStore.isOnboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName: StateFlow<String> = dataStore.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val biometricEnabled: StateFlow<Boolean> = dataStore.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val onboarded = dataStore.isOnboarded.first()
            val bioEnabled = dataStore.biometricEnabled.first()
            isUnlocked.value = !onboarded || !bioEnabled
            EchoCareTelemetry.logEvent("auth_viewmodel_initialized", mapOf("onboarded" to onboarded, "biometric_enabled" to bioEnabled))
        }
    }

    fun completeOnboarding(name: String) {
        viewModelScope.launch {
            dataStore.setUserName(name.trim())
            dataStore.setOnboarded(true)
            isUnlocked.value = true
        }
    }

    fun setUnlocked(unlocked: Boolean) {
        isUnlocked.value = unlocked
        EchoCareTelemetry.logEvent("app_unlock_status_changed", mapOf("unlocked" to unlocked))
    }
}

