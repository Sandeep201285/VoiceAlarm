package com.echocare.app.domain.model

data class Group(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: Long = System.currentTimeMillis()
)
