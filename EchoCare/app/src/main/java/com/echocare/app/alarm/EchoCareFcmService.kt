package com.echocare.app.alarm

import android.util.Log
import com.echocare.app.data.repository.ReminderRepository
import com.echocare.app.domain.model.RecurrenceType
import com.echocare.app.domain.model.Reminder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EchoCareFcmService : FirebaseMessagingService() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "EchoCareFcmService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
        // Token upload would be handled by Auth/User Session syncing
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received: ${remoteMessage.data}")

        val data = remoteMessage.data
        val type = data["type"] ?: "REMINDER"

        if (type == "REMINDER") {
            handleReminderPayload(data)
        }
    }

    private fun handleReminderPayload(data: Map<String, String>) {
        val idStr           = data["id"] ?: return
        val title           = data["title"] ?: "Shared Reminder"
        val message         = data["message"] ?: ""
        val triggerTimeStr  = data["triggerTime"] ?: return
        val recTypeStr      = data["recurrenceType"] ?: "ONCE"
        val recIntStr       = data["recurrenceIntervalMs"] ?: "0"
        val creatorId       = data["creatorId"] ?: ""
        val recipientId     = data["recipientId"]
        val repeatIntStr    = data["repeatIntervalMinutes"] ?: "5"
        val maxRepStr       = data["maxRepetitions"] ?: "3"

        serviceScope.launch {
            try {
                val reminder = Reminder(
                    id = idStr.toLongOrNull() ?: System.currentTimeMillis(),
                    title = title,
                    message = message,
                    triggerTime = triggerTimeStr.toLongOrNull() ?: System.currentTimeMillis(),
                    recurrenceType = RecurrenceType.valueOf(recTypeStr),
                    recurrenceIntervalMs = recIntStr.toLongOrNull() ?: 0L,
                    creatorId = creatorId,
                    recipientId = recipientId,
                    repeatIntervalMinutes = repeatIntStr.toIntOrNull() ?: 5,
                    maxRepetitions = maxRepStr.toIntOrNull() ?: 3,
                    syncStatus = "SYNCED",
                    isActive = true
                )

                // Save to encrypted Room DB
                reminderRepository.insertReminder(reminder)

                // Schedule Exact Alarm locally
                alarmScheduler.schedule(reminder)
                
                Log.d(TAG, "Successfully processed FCM remote reminder: ${reminder.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse FCM reminder payload", e)
            }
        }
    }
}
