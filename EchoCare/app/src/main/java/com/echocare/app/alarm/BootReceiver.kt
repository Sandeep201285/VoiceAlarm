package com.echocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.echocare.app.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Re-schedules all active, future reminders when the device reboots.
 * Android cancels all AlarmManager entries on reboot — this receiver restores them.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
        if (intent.action !in validActions) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // .first() gets the current DB state in one shot — no need to keep collecting
                val reminders = reminderRepository.getAllReminders().first()
                val now       = System.currentTimeMillis()

                reminders
                    .filter { it.isActive && it.triggerTime > now }
                    .forEach { alarmScheduler.schedule(it) }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to reschedule alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
