package com.echocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Receives the AlarmManager broadcast and hands off to [AlarmService].
 * Kept intentionally thin — all business logic lives in the service.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.echocare.app.ACTION_ALARM_TRIGGER"
        const val EXTRA_REMINDER_ID    = "extra_reminder_id"
        const val EXTRA_REPEAT_COUNT   = "extra_repeat_count"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGER) return

        val reminderId  = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 0)

        if (reminderId == -1L) return

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REPEAT_COUNT, repeatCount)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
