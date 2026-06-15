package com.echocare.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echocare.app.data.repository.ReminderRepository
import com.echocare.app.util.BackupHelper
import com.echocare.app.util.DataStoreHelper
import com.echocare.app.util.EchoCareTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String          = "",
    val ttsLanguage: String       = "en",
    val ttsSpeed: Float           = 1.0f,
    val biometricEnabled: Boolean = false,
    val repeatIntervalMin: Int    = 5,
    val maxRepetitions: Int       = 3,
    val isPremium: Boolean        = false,
    val voicePitch: Float         = 1.0f,
    val alexaLinked: Boolean      = false,
    val googleHomeLinked: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStoreHelper,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        dataStore.userName,
        dataStore.ttsLanguage,
        dataStore.ttsSpeed,
        dataStore.biometricEnabled,
        dataStore.defaultRepeatInterval,
        dataStore.defaultMaxRepetitions,
        dataStore.isPremium,
        dataStore.voicePitch,
        dataStore.alexaLinked,
        dataStore.googleHomeLinked
    ) { flows: Array<Any> ->
        SettingsUiState(
            userName          = flows[0] as String,
            ttsLanguage       = flows[1] as String,
            ttsSpeed          = flows[2] as Float,
            biometricEnabled  = flows[3] as Boolean,
            repeatIntervalMin = flows[4] as Int,
            maxRepetitions    = flows[5] as Int,
            isPremium         = flows[6] as Boolean,
            voicePitch        = flows[7] as Float,
            alexaLinked       = flows[8] as Boolean,
            googleHomeLinked  = flows[9] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun updateName(name: String)           { viewModelScope.launch { dataStore.setUserName(name) } }
    fun updateLanguage(lang: String)       { viewModelScope.launch { dataStore.setTtsLanguage(lang) } }
    fun updateSpeed(speed: Float)          { viewModelScope.launch { dataStore.setTtsSpeed(speed) } }
    fun toggleBiometric(enabled: Boolean)  { viewModelScope.launch { dataStore.setBiometricEnabled(enabled) } }
    fun updateRepeatInterval(min: Int)     { viewModelScope.launch { dataStore.setDefaultRepeatInterval(min) } }
    fun updateMaxRepetitions(count: Int)   { viewModelScope.launch { dataStore.setDefaultMaxRepetitions(count) } }
    fun updatePremium(premium: Boolean)    { viewModelScope.launch { dataStore.setPremium(premium) } }
    fun updateVoicePitch(pitch: Float)     { viewModelScope.launch { dataStore.setVoicePitch(pitch) } }
    fun toggleAlexa(linked: Boolean)       { viewModelScope.launch { dataStore.setAlexaLinked(linked) } }
    fun toggleGoogleHome(linked: Boolean)  { viewModelScope.launch { dataStore.setGoogleHomeLinked(linked) } }

    fun backupReminders(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val reminders = reminderRepository.getUpcomingReminders().first() + reminderRepository.getPastReminders().first()
                val json = BackupHelper.exportToBackup(reminders)
                val file = java.io.File(context.getExternalFilesDir(null), "echocare_backup.json")
                file.writeText(json)
                EchoCareTelemetry.logEvent("database_backup_exported", mapOf("count" to reminders.size, "path" to file.name))
                onSuccess(file.absolutePath)
            } catch (e: Exception) {
                EchoCareTelemetry.logException(e, false)
                onError(e.message ?: "Backup failed")
            }
        }
    }

    fun restoreReminders(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = java.io.File(context.getExternalFilesDir(null), "echocare_backup.json")
                if (!file.exists()) {
                    onError("No backup file found at ${file.name}")
                    return@launch
                }
                val json = file.readText()
                val reminders = BackupHelper.importFromBackup(json)
                if (reminders.isEmpty()) {
                    onError("Backup file is empty or invalid")
                    return@launch
                }
                for (r in reminders) {
                    reminderRepository.insertReminder(r)
                }
                EchoCareTelemetry.logEvent("database_backup_imported", mapOf("count" to reminders.size))
                onSuccess()
            } catch (e: Exception) {
                EchoCareTelemetry.logException(e, false)
                onError(e.message ?: "Restore failed")
            }
        }
    }

    fun triggerNonFatalCrash() {
        val simulatedException = Exception("Simulated Non-Fatal Exception in Settings UI")
        EchoCareTelemetry.logException(simulatedException, fatal = false)
    }

    fun triggerFatalCrash() {
        val crash = RuntimeException("FATAL: Simulated Fatal Crash from developer panel")
        EchoCareTelemetry.logException(crash, fatal = true)
        throw crash
    }

    fun clearTelemetryLogs() {
        EchoCareTelemetry.clearLogs()
    }
}
