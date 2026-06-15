package com.echocare.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "userId"]
)
data class GroupMemberEntity(
    val groupId: String,
    val userId: String,
    val name: String,
    val phone: String,
    val role: String, // OWNER, ADMIN, MEMBER
    val status: String // PENDING, ACCEPTED
)
