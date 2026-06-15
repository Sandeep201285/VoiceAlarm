package com.echocare.app.data.repository

import com.echocare.app.data.local.dao.DeliveryLogDao
import com.echocare.app.data.local.dao.ReminderDao
import com.echocare.app.data.local.entity.DeliveryLogEntity
import com.echocare.app.data.local.entity.ReminderEntity
import com.echocare.app.domain.model.Reminder
import com.echocare.app.domain.model.RecurrenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val deliveryLogDao: DeliveryLogDao
) {
    // ─── Reminder CRUD ───────────────────────────────────────────────────────

    fun getUpcomingReminders(): Flow<List<Reminder>> =
        reminderDao.getUpcomingReminders().map { list -> list.map { it.toDomain() } }

    fun getPastReminders(): Flow<List<Reminder>> =
        reminderDao.getPastReminders().map { list -> list.map { it.toDomain() } }

    fun getAllReminders(): Flow<List<Reminder>> =
        reminderDao.getAllReminders().map { list -> list.map { it.toDomain() } }

    suspend fun getReminderById(id: Long): Reminder? =
        reminderDao.getReminderById(id)?.toDomain()

    suspend fun insertReminder(reminder: Reminder): Long =
        reminderDao.insertReminder(reminder.toEntity())

    suspend fun updateReminder(reminder: Reminder) =
        reminderDao.updateReminder(reminder.toEntity())

    suspend fun deleteReminder(id: Long) =
        reminderDao.deleteReminderById(id)

    suspend fun setActive(id: Long, isActive: Boolean) =
        reminderDao.setReminderActive(id, isActive)

    suspend fun updateTriggerTime(id: Long, newTime: Long) =
        reminderDao.updateTriggerTime(id, newTime)

    // ─── Delivery Logs ───────────────────────────────────────────────────────

    suspend fun logDelivery(reminderId: Long, triggeredAt: Long): Long =
        deliveryLogDao.insertLog(
            DeliveryLogEntity(reminderId = reminderId, triggeredAt = triggeredAt)
        )

    suspend fun acknowledgeDelivery(logId: Long) =
        deliveryLogDao.acknowledge(logId)

    // ─── Mapping Helpers ─────────────────────────────────────────────────────

    private fun ReminderEntity.toDomain() = Reminder(
        id                   = id,
        title                = title,
        message              = message,
        triggerTime          = triggerTime,
        recurrenceType       = RecurrenceType.valueOf(recurrenceType),
        recurrenceIntervalMs = recurrenceIntervalMs,
        isActive             = isActive,
        repeatIntervalMinutes= repeatIntervalMinutes,
        maxRepetitions       = maxRepetitions,
        creatorId            = creatorId,
        recipientId          = recipientId,
        syncStatus           = syncStatus,
        createdAt            = createdAt
    )

    private fun Reminder.toEntity() = ReminderEntity(
        id                   = id,
        title                = title,
        message              = message,
        triggerTime          = triggerTime,
        recurrenceType       = recurrenceType.name,
        recurrenceIntervalMs = recurrenceIntervalMs,
        isActive             = isActive,
        repeatIntervalMinutes= repeatIntervalMinutes,
        maxRepetitions       = maxRepetitions,
        creatorId            = creatorId,
        recipientId          = recipientId,
        syncStatus           = syncStatus,
        createdAt            = createdAt
    )
}
