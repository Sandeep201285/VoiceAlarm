package com.echocare.app.data.repository

import com.echocare.app.data.local.dao.GroupDao
import com.echocare.app.data.local.dao.GroupMemberDao
import com.echocare.app.data.local.entity.GroupEntity
import com.echocare.app.data.local.entity.GroupMemberEntity
import com.echocare.app.domain.model.Group
import com.echocare.app.domain.model.GroupMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val groupMemberDao: GroupMemberDao
) {
    fun getAllGroups(): Flow<List<Group>> =
        groupDao.getAllGroups().map { list -> list.map { it.toDomain() } }

    fun getMembersForGroup(groupId: String): Flow<List<GroupMember>> =
        groupMemberDao.getMembersForGroup(groupId).map { list -> list.map { it.toDomain() } }

    suspend fun getGroupById(groupId: String): Group? =
        groupDao.getGroupById(groupId)?.toDomain()

    suspend fun createGroup(name: String, ownerId: String, members: List<GroupMember>) {
        val groupId = UUID.randomUUID().toString()
        val groupEntity = GroupEntity(id = groupId, name = name, ownerId = ownerId)
        
        groupDao.insertGroup(groupEntity)
        
        // Add owner as accepted member
        val ownerMember = GroupMemberEntity(
            groupId = groupId,
            userId = ownerId,
            name = "You",
            phone = "",
            role = "OWNER",
            status = "ACCEPTED"
        )
        groupMemberDao.insertMember(ownerMember)

        // Add guest members as pending
        val memberEntities = members.map {
            GroupMemberEntity(
                groupId = groupId,
                userId = it.userId.ifBlank { UUID.randomUUID().toString() },
                name = it.name,
                phone = it.phone,
                role = "MEMBER",
                status = "PENDING"
            )
        }
        groupMemberDao.insertMembers(memberEntities)
    }

    suspend fun insertGroupDirect(group: Group) {
        groupDao.insertGroup(group.toEntity())
    }

    suspend fun insertMembersDirect(members: List<GroupMember>) {
        groupMemberDao.insertMembers(members.map { it.toEntity() })
    }

    suspend fun updateMemberStatus(groupId: String, userId: String, status: String) {
        groupMemberDao.getMember(groupId, userId)?.let {
            groupMemberDao.insertMember(it.copy(status = status))
        }
    }

    suspend fun deleteGroup(groupId: String) {
        groupDao.getGroupById(groupId)?.let {
            groupDao.deleteGroup(it)
            groupMemberDao.clearGroupMembers(groupId)
        }
    }

    // ─── Mapping Helpers ─────────────────────────────────────────────────────

    private fun GroupEntity.toDomain() = Group(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt
    )

    private fun Group.toEntity() = GroupEntity(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt
    )

    private fun GroupMemberEntity.toDomain() = GroupMember(
        groupId = groupId,
        userId = userId,
        name = name,
        phone = phone,
        role = role,
        status = status
    )

    private fun GroupMember.toEntity() = GroupMemberEntity(
        groupId = groupId,
        userId = userId,
        name = name,
        phone = phone,
        role = role,
        status = status
    )
}
