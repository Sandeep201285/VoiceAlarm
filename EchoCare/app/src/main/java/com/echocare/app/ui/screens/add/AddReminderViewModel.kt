package com.echocare.app.ui.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echocare.app.alarm.AlarmScheduler
import com.echocare.app.data.repository.ReminderRepository
import com.echocare.app.domain.model.Reminder
import com.echocare.app.domain.model.RecurrenceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddReminderUiState(
    val title: String               = "",
    val message: String             = "",
    val triggerTimeMs: Long         = System.currentTimeMillis() + 60_000L,
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    val recurrenceIntervalHours: Int = 1,
    val repeatIntervalMinutes: Int  = 5,
    val maxRepetitions: Int         = 3,
    val isEditMode: Boolean         = false,
    val isSaving: Boolean           = false,
    val savedSuccessfully: Boolean  = false,
    val error: String?              = null
)

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reminderId: Long = savedStateHandle.get<Long>("reminderId") ?: -1L

    private val _uiState = MutableStateFlow(AddReminderUiState())
    val uiState: StateFlow<AddReminderUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != -1L) {
            viewModelScope.launch {
                reminderRepository.getReminderById(reminderId)?.let { existing ->
                    _uiState.update {
                        it.copy(
                            title                   = existing.title,
                            message                 = existing.message,
                            triggerTimeMs           = existing.triggerTime,
                            recurrenceType          = existing.recurrenceType,
                            recurrenceIntervalHours = (existing.recurrenceIntervalMs / 3_600_000L).toInt().coerceAtLeast(1),
                            repeatIntervalMinutes   = existing.repeatIntervalMinutes,
                            maxRepetitions          = existing.maxRepetitions,
                            isEditMode              = true
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(v: String)              = _uiState.update { it.copy(title = v, error = null) }
    fun updateMessage(v: String)            = _uiState.update { it.copy(message = v, error = null) }
    fun updateTriggerTime(ms: Long)         = _uiState.update { it.copy(triggerTimeMs = ms) }
    fun updateRecurrenceType(v: RecurrenceType) = _uiState.update { it.copy(recurrenceType = v) }
    fun updateRecurrenceInterval(h: Int)   = _uiState.update { it.copy(recurrenceIntervalHours = h) }
    fun updateRepeatInterval(m: Int)        = _uiState.update { it.copy(repeatIntervalMinutes = m) }
    fun updateMaxRepetitions(n: Int)        = _uiState.update { it.copy(maxRepetitions = n) }

    fun save() {
        val s = _uiState.value
        if (s.title.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a title") }; return
        }
        if (s.message.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a reminder message") }; return
        }
        if (s.triggerTimeMs <= System.currentTimeMillis()) {
            _uiState.update { it.copy(error = "Please choose a future time") }; return
        }

        _uiState.update { it.copy(isSaving = true) }

        val reminder = Reminder(
            id                   = if (s.isEditMode) reminderId else 0,
            title                = s.title.trim(),
            message              = s.message.trim(),
            triggerTime          = s.triggerTimeMs,
            recurrenceType       = s.recurrenceType,
            recurrenceIntervalMs = s.recurrenceIntervalHours * 3_600_000L,
            repeatIntervalMinutes= s.repeatIntervalMinutes,
            maxRepetitions       = s.maxRepetitions,
            isActive             = true
        )

        viewModelScope.launch {
            try {
                val savedId = if (s.isEditMode) {
                    alarmScheduler.cancel(reminderId)
                    reminderRepository.updateReminder(reminder)
                    reminderId
                } else {
                    reminderRepository.insertReminder(reminder)
                }
                alarmScheduler.schedule(reminder.copy(id = savedId))
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Save failed: ${e.message}") }
            }
        }
    }
}
