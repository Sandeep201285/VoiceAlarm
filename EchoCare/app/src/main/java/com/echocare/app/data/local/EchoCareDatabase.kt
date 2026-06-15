package com.echocare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.echocare.app.data.local.dao.DeliveryLogDao
import com.echocare.app.data.local.dao.ReminderDao
import com.echocare.app.data.local.dao.GroupDao
import com.echocare.app.data.local.dao.GroupMemberDao
import com.echocare.app.data.local.entity.DeliveryLogEntity
import com.echocare.app.data.local.entity.ReminderEntity
import com.echocare.app.data.local.entity.GroupEntity
import com.echocare.app.data.local.entity.GroupMemberEntity

@Database(
    entities = [
        ReminderEntity::class,
        DeliveryLogEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EchoCareDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun deliveryLogDao(): DeliveryLogDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
}
