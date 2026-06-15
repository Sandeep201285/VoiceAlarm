package com.echocare.app.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import com.echocare.app.data.repository.ReminderRepository
import com.echocare.app.domain.model.Reminder
import com.echocare.app.ui.screens.alarm.AlarmTriggerActivity
import com.echocare.app.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Locale
import javax.inject.Inject

/**
 * Foreground Service responsible for:
 * 1. Showing a persistent notification (Android 8+ requirement)
 * 2. Launching [AlarmTriggerActivity] full-screen on lock screen
 * 3. Speaking the reminder via Android TTS
 * 4. Scheduling repeat alarms until the user acknowledges
 */
@AndroidEntryPoint
class AlarmService : Service() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var notificationHelper: NotificationHelper

    private var tts: TextToSpeech? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_ACKNOWLEDGE = "com.echocare.app.ACTION_ACKNOWLEDGE"
        private const val TAG = "AlarmService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground notification must be posted immediately on Android 8+
        startForeground(
            NotificationHelper.NOTIF_FOREGROUND_ID,
            notificationHelper.buildForegroundNotification("EchoCare", "Processing reminder…")
        )

        when (intent?.action) {
            AlarmReceiver.ACTION_ALARM_TRIGGER -> handleAlarmTrigger(intent)
            ACTION_ACKNOWLEDGE                 -> handleAcknowledge(intent)
            else                               -> stopSelf()
        }
        return START_NOT_STICKY
    }

    // ─── Alarm Trigger ────────────────────────────────────────────────────────

    private fun handleAlarmTrigger(intent: Intent) {
        val reminderId  = intent.getLongExtra(AlarmReceiver.EXTRA_REMINDER_ID, -1L)
        val repeatCount = intent.getIntExtra(AlarmReceiver.EXTRA_REPEAT_COUNT, 0)

        if (reminderId == -1L) { stopSelf(); return }

        scope.launch {
            val reminder = reminderRepository.getReminderById(reminderId)
            if (reminder == null || !reminder.isActive) {
                Log.w(TAG, "Reminder $reminderId not found or inactive")
                stopSelf(); return@launch
            }

            val logId = reminderRepository.logDelivery(reminderId, System.currentTimeMillis())

            withContext(Dispatchers.Main) {
                launchAlarmScreen(reminder)
                showAlarmNotification(reminder, logId)
                speakReminder(reminder)

                // Schedule next repeat if user hasn't acknowledged yet
                if (reminder.maxRepetitions == 0 || repeatCount < reminder.maxRepetitions - 1) {
                    alarmScheduler.scheduleRepeat(reminder, repeatCount + 1)
                }

                // Schedule next recurrence (only on initial trigger, not on repeats)
                if (repeatCount == 0) {
                    alarmScheduler.scheduleNextRecurrence(reminder)?.let { updated ->
                        reminderRepository.updateTriggerTime(reminderId, updated.triggerTime)
                    }
                }
            }
        }
    }

    // ─── Acknowledge ──────────────────────────────────────────────────────────

    private fun handleAcknowledge(intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmReceiver.EXTRA_REMINDER_ID, -1L)
        val logId      = intent.getLongExtra("extra_log_id", -1L)

        if (reminderId != -1L) {
            if (logId != -1L) scope.launch { reminderRepository.acknowledgeDelivery(logId) }
            alarmScheduler.cancel(reminderId)
            notificationHelper.cancelNotification(reminderId)
        }

        tts?.stop()

        @Suppress("DEPRECATION")
        stopForeground(true)  // Works on all API levels (deprecation OK for API 26–32)
        stopSelf()
    }

    // ─── Full-Screen Activity ─────────────────────────────────────────────────

    private fun launchAlarmScreen(reminder: Reminder) {
        Intent(this, AlarmTriggerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra("extra_reminder_title",   reminder.title)
            putExtra("extra_reminder_message", reminder.message)
        }.also { startActivity(it) }
    }

    // ─── Notification with Acknowledge action ─────────────────────────────────

    private fun showAlarmNotification(reminder: Reminder, logId: Long) {
        val ackPendingIntent = PendingIntent.getService(
            this,
            reminder.id.toInt(),
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_ACKNOWLEDGE
                putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra("extra_log_id", logId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationHelper.showAlarmNotification(
            reminderId = reminder.id,
            title      = reminder.title,
            message    = reminder.message,
            acknowledgePendingIntent = ackPendingIntent
        )
    }

    // ─── TTS Playback ─────────────────────────────────────────────────────────

    private fun speakReminder(reminder: Reminder) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(1.0f)
                tts?.speak(reminder.message, TextToSpeech.QUEUE_FLUSH, null, reminder.id.toString())
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        scope.cancel()
        super.onDestroy()
    }
}
