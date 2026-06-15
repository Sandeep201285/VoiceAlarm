package com.echocare.app.domain.model

/**
 * Domain model for a reminder.
 * Used across the app (UI, ViewModel, Use Cases).
 * Mapped to/from [ReminderEntity] for persistence.
 */
data class Reminder(
    val id: Long = 0,
    /** Short display title, e.g. "Morning Medicine" */
    val title: String,
    /** The exact text that TTS will speak aloud */
    val message: String,
    /** Next trigger time in epoch milliseconds */
    val triggerTime: Long,
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    /** Custom recurrence interval in milliseconds (used when type == CUSTOM) */
    val recurrenceIntervalMs: Long = 0L,
    val isActive: Boolean = true,
    /** Minutes between repeat announcements until acknowledged */
    val repeatIntervalMinutes: Int = 5,
    /** Max number of repeat announcements (0 = unlimited) */
    val maxRepetitions: Int = 3,
    val creatorId: String = "",
    val recipientId: String? = null,
    val syncStatus: String = "SYNCED",
    val createdAt: Long = System.currentTimeMillis()
)
