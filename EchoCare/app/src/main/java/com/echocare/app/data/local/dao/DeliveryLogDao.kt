package com.echocare.app.data.local.dao

import androidx.room.*
import com.echocare.app.data.local.entity.DeliveryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DeliveryLogEntity): Long

    @Query("SELECT * FROM delivery_logs WHERE reminderId = :reminderId ORDER BY triggeredAt DESC")
    fun getLogsForReminder(reminderId: Long): Flow<List<DeliveryLogEntity>>

    @Query("UPDATE delivery_logs SET acknowledgedAt = :ackTime, result = 'SUCCESS' WHERE id = :logId")
    suspend fun acknowledge(logId: Long, ackTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM delivery_logs WHERE reminderId = :reminderId")
    suspend fun deleteLogsForReminder(reminderId: Long)
}
