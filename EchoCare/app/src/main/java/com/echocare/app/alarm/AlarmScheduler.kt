package com.echocare.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.echocare.app.domain.model.Reminder
import com.echocare.app.domain.model.RecurrenceType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context   // ← fixed: @ApplicationContext
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ─── Schedule the primary alarm for a reminder ────────────────────────

    fun schedule(reminder: Reminder) {
        if (!reminder.isActive) return
        val pendingIntent = buildPendingIntent(reminder.id, 0, reminder.id.toInt())
        setExactAlarm(reminder.triggerTime, pendingIntent)
    }

    // ─── Schedule a repeat alarm (until acknowledged) ─────────────────────

    fun scheduleRepeat(reminder: Reminder, repeatCount: Int) {
        if (reminder.maxRepetitions > 0 && repeatCount >= reminder.maxRepetitions) return
        val triggerAt  = System.currentTimeMillis() + reminder.repeatIntervalMinutes * 60_000L
        val requestCode = (reminder.id * 1000 + repeatCount).toInt()
        setExactAlarm(triggerAt, buildPendingIntent(reminder.id, repeatCount, requestCode))
    }

    // ─── Cancel all alarm slots for a reminder ────────────────────────────

    fun cancel(reminderId: Long) {
        cancelByRequestCode(reminderId.toInt())
        for (i in 0..10) cancelByRequestCode((reminderId * 1000 + i).toInt())
    }

    // ─── Calculate & schedule next recurrence; returns updated reminder ───

    fun scheduleNextRecurrence(reminder: Reminder): Reminder? {
        val nextTime = nextTriggerTime(reminder) ?: return null
        val updated  = reminder.copy(triggerTime = nextTime)
        schedule(updated)
        return updated
    }

    // ─── Android 12+ exact-alarm permission check ─────────────────────────

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms()
        else true

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun buildPendingIntent(
        reminderId: Long, repeatCount: Int, requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_REPEAT_COUNT, repeatCount)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setExactAlarm(triggerAtMs: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        else
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
    }

    private fun cancelByRequestCode(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { alarmManager.cancel(it); it.cancel() }
    }

    private fun nextTriggerTime(reminder: Reminder): Long? =
        when (reminder.recurrenceType) {
            RecurrenceType.ONCE    -> null
            RecurrenceType.DAILY   -> reminder.triggerTime + 86_400_000L
            RecurrenceType.WEEKLY  -> reminder.triggerTime + 7 * 86_400_000L
            RecurrenceType.MONTHLY -> Calendar.getInstance().also {
                it.timeInMillis = reminder.triggerTime
                it.add(Calendar.MONTH, 1)
            }.timeInMillis
            RecurrenceType.CUSTOM  -> reminder.triggerTime + reminder.recurrenceIntervalMs
        }
}
