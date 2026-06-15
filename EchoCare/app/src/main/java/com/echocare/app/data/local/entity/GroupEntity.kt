package com.echocare.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: Long = System.currentTimeMillis()
)
