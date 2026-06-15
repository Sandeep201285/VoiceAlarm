package com.echocare.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echocare.app.alarm.AlarmScheduler
import com.echocare.app.data.repository.ReminderRepository
import com.echocare.app.domain.model.Reminder
import com.echocare.app.domain.model.RecurrenceType
import com.echocare.app.util.DataStoreHelper
import com.echocare.app.util.NlpParser
import com.echocare.app.util.SpeechRecognizerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeechUiState(
    val showVoiceInputOverlay: Boolean = false,
    val showParsedConfirmation: Boolean = false,
    val parsedReminder: Reminder? = null
)

data class HomeUiState(
    val userName: String           = "",
    val upcomingReminders: List<Reminder> = emptyList(),
    val pastReminders: List<Reminder>     = emptyList(),
    val isLoading: Boolean         = true,
    
    // Voice Capture States
    val showVoiceInputOverlay: Boolean = false,
    val isListening: Boolean           = false,
    val voiceTranscript: String        = "",
    val voiceRmsDecibels: Float        = 0f,
    val speechError: String?           = null,
    
    // Confirmation States
    val showParsedConfirmation: Boolean = false,
    val parsedReminder: Reminder?      = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val dataStore: DataStoreHelper,
    private val speechHelper: SpeechRecognizerHelper
) : ViewModel() {

    private val _speechState = MutableStateFlow(SpeechUiState())

    // Combine local preferences, database flows, and speech flows into a single unified HomeUiState
    val uiState: StateFlow<HomeUiState> = combine(
        dataStore.userName,
        reminderRepository.getUpcomingReminders(),
        reminderRepository.getPastReminders(),
        _speechState,
        speechHelper.isListening,
        speechHelper.partialTranscript,
        speechHelper.rmsDb,
        speechHelper.error
    ) { name, upcoming, past, speech, isListening, partial, rms, error ->
        HomeUiState(
            userName = name,
            upcomingReminders = upcoming,
            pastReminders = past,
            isLoading = false,
            
            showVoiceInputOverlay = speech.showVoiceInputOverlay,
            isListening = isListening,
            voiceTranscript = partial,
            voiceRmsDecibels = rms,
            speechError = error,
            
            showParsedConfirmation = speech.showParsedConfirmation,
            parsedReminder = speech.parsedReminder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        // Observe final transcript results to trigger parsing automatically
        viewModelScope.launch {
            speechHelper.finalTranscript.collect { text ->
                if (text.isNotBlank() && _speechState.value.showVoiceInputOverlay) {
                    processTranscript(text)
                }
            }
        }

        // Auto-process if speech finishes with no results but speech helper stopped listening
        viewModelScope.launch {
            combine(speechHelper.isListening, speechHelper.error) { listening, error ->
                Pair(listening, error)
            }.collect { (listening, error) ->
                val state = _speechState.value
                // If stopped listening and has error (like speech timeout), expose it.
                // If it stopped naturally and we have a partial result but no final result yet, fallback to parsing partial transcript.
                if (!listening && state.showVoiceInputOverlay && error == null) {
                    val partialText = speechHelper.partialTranscript.value
                    if (partialText.isNotBlank() && speechHelper.finalTranscript.value.isBlank()) {
                        processTranscript(partialText)
                    }
                }
            }
        }
    }

    fun startListening() {
        viewModelScope.launch {
            _speechState.value = SpeechUiState(showVoiceInputOverlay = true)
            val lang = dataStore.ttsLanguage.first()
            val speechLocale = if (lang == "hi") "hi-IN" else "en-IN"
            speechHelper.startListening(speechLocale)
        }
    }

    fun stopListening() {
        speechHelper.stopListening()
    }

    fun cancelListening() {
        speechHelper.cancel()
        _speechState.value = SpeechUiState()
    }

    fun processTranscript(text: String) {
        viewModelScope.launch {
            speechHelper.cancel() // Stop recording
            
            // Get user default preference settings
            val repeatInterval = dataStore.defaultRepeatInterval.first()
            val maxRepetitions = dataStore.defaultMaxRepetitions.first()

            val nlpResult = NlpParser.parse(text)
            val tempReminder = Reminder(
                title = nlpResult.title,
                message = nlpResult.message,
                triggerTime = nlpResult.triggerTime,
                recurrenceType = nlpResult.recurrenceType,
                recurrenceIntervalMs = nlpResult.recurrenceIntervalMs,
                repeatIntervalMinutes = repeatInterval,
                maxRepetitions = maxRepetitions,
                isActive = true
            )
            
            _speechState.value = SpeechUiState(
                showVoiceInputOverlay = false,
                showParsedConfirmation = true,
                parsedReminder = tempReminder
            )
        }
    }

    fun updateParsedReminder(reminder: Reminder) {
        _speechState.update { it.copy(parsedReminder = reminder) }
    }

    fun confirmParsedReminder() {
        val reminder = _speechState.value.parsedReminder ?: return
        viewModelScope.launch {
            val id = reminderRepository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = id)
            alarmScheduler.schedule(savedReminder)
            _speechState.value = SpeechUiState() // Dismiss confirmation
        }
    }

    fun dismissConfirmation() {
        _speechState.value = SpeechUiState()
    }

    fun toggleActive(reminder: Reminder) {
        viewModelScope.launch {
            val newActive = !reminder.isActive
            reminderRepository.setActive(reminder.id, newActive)
            if (newActive) alarmScheduler.schedule(reminder.copy(isActive = true))
            else           alarmScheduler.cancel(reminder.id)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder.id)
            reminderRepository.deleteReminder(reminder.id)
        }
    }

    override fun onCleared() {
        speechHelper.cleanup()
        super.onCleared()
    }
}
