package com.echocare.app.data.remote

import retrofit2.http.*

data class UserDto(val uid: String, val name: String, val phone: String, val fcmToken: String)
data class GroupDto(val id: String, val name: String, val ownerId: String)
data class GroupInviteDto(val groupId: String, val phone: String, val name: String)
data class ReminderDto(
    val id: Long,
    val title: String,
    val message: String,
    val triggerTime: Long,
    val recurrenceType: String,
    val recurrenceIntervalMs: Long,
    val creatorId: String,
    val recipientId: String?,
    val repeatIntervalMinutes: Int,
    val maxRepetitions: Int
)

interface EchoCareApiService {
    @POST("users/register")
    suspend fun registerUser(@Body user: UserDto)

    @POST("groups")
    suspend fun createGroup(@Body group: GroupDto): GroupDto

    @GET("groups")
    suspend fun getGroups(@Query("userId") userId: String): List<GroupDto>

    @POST("groups/invite")
    suspend fun sendInvitation(@Body invite: GroupInviteDto)

    @POST("reminders/sync")
    suspend fun syncReminders(
        @Query("userId") userId: String,
        @Body localReminders: List<ReminderDto>
    ): List<ReminderDto>

    @POST("reminders/{id}/acknowledge")
    suspend fun acknowledgeReminder(
        @Path("id") reminderId: Long,
        @Query("acknowledgedAt") acknowledgedAt: Long
    )
}
