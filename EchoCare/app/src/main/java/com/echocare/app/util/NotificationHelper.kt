package com.echocare.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.echocare.app.MainActivity
import com.echocare.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context   // ← fixed: @ApplicationContext
) {
    companion object {
        const val CHANNEL_ALARM        = "echocare_alarm"
        const val CHANNEL_SERVICE      = "echocare_service"
        const val NOTIF_FOREGROUND_ID  = 1001
    }

    init { createChannels() }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            NotificationChannel(
                CHANNEL_ALARM, "Reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "EchoCare reminder alerts"
                enableVibration(true)
                setShowBadge(true)
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                CHANNEL_SERVICE, "EchoCare Running", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "EchoCare alarm service"
                manager.createNotificationChannel(this)
            }
        }
    }

    /** Foreground service notification — keeps AlarmService alive */
    fun buildForegroundNotification(title: String, message: String) =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    /** High-priority alarm notification with Acknowledge action */
    fun showAlarmNotification(
        reminderId: Long,
        title: String,
        message: String,
        acknowledgePendingIntent: PendingIntent
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_alarm, "Acknowledge", acknowledgePendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(reminderId.toInt(), notification)
    }

    fun cancelNotification(reminderId: Long) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(reminderId.toInt())
    }
}
