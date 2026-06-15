package com.echocare.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    /** Next trigger time in epoch millis */
    val triggerTime: Long,
    /** One of: ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM */
    val recurrenceType: String,
    /** Custom recurrence interval in millis */
    val recurrenceIntervalMs: Long = 0L,
    val isActive: Boolean = true,
    val repeatIntervalMinutes: Int = 5,
    val maxRepetitions: Int = 3,
    val creatorId: String = "",
    val recipientId: String? = null,
    val syncStatus: String = "SYNCED",
    val createdAt: Long = System.currentTimeMillis()
)
