package com.echocare.app.domain.model

data class GroupMember(
    val groupId: String,
    val userId: String,
    val name: String,
    val phone: String,
    val role: String, // OWNER, ADMIN, MEMBER
    val status: String // PENDING, ACCEPTED
)
